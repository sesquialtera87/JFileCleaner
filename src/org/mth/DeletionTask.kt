package org.mth

import java.awt.Desktop
import java.io.File
import java.io.IOException
import javax.swing.SwingWorker


class DeletionTask(val directory: File, val extensions: Set<String>, val toTrash: Boolean, val recursive: Boolean) :
    SwingWorker<Unit, DeletionTask.DeletionOutcome>() {

    var deletionCounter = 0

    override fun doInBackground() {
        Logger.message(formattedTimeStamp(), Logger.timestampStyle)

        recursiveDeletion(directory)
    }

    override fun done() {
        if (isCancelled)
            Logger.message("Deletion task cancelled", Logger.warningStyle)

        Logger.message("$deletionCounter files deleted\n", Logger.greenStyle)
    }

    private fun recursiveDeletion(path: File) {
        val fileCollection = path.listFiles()

        if (fileCollection != null) {
            val (files, directories) = fileCollection.partition { it.isFile }

            if (toTrash)
                files.filter { it.extension() in extensions }
                    .forEach { it.toTrash() }
            else
                deleteFiles(files.filter { it.extension() in extensions })

            // proceed in recursion
            if (recursive)
                for (dir in directories) {
                    if (isCancelled) {
                        System.err.println("Task canceled")
                        break
                    }

                    recursiveDeletion(dir)
                }
        }
    }

    private fun deleteFiles(files: List<File>) {
        val bold = Logger.defaultStyle.bold()

        for (it in files) {
            if (isCancelled) {
                System.err.println("Task canceled")
                break
            }

            try {
                val succeeded = it.delete()

                if (succeeded) {
                    deletionCounter++
                    Logger.append("- ")
                        .append(it.name, bold)
                        .append(" deleted\n")
                } else
                    Logger.message("- Cannot delete ${it.name}")
            } catch (_: IOException) {
                Logger.message("- Cannot delete ${it.name}")
            } catch (_: SecurityException) {
                Logger.message("- Cannot delete ${it.name}")
            }
        }
    }

    override fun process(chunks: MutableList<DeletionOutcome>) {
        val bold = Logger.defaultStyle.bold()

        chunks.forEach {
            if (it.deletionDone) {
                deletionCounter++

                Logger.append("- ")
                    .append(it.file.name, bold)
                    .append(" deleted\n")
            } else
                Logger.message("- Cannot delete ${it.file.name}")
        }
    }

    /**
     * Move a file to the system trash
     */
    private fun File.toTrash(): Boolean {
        try {
            val succeeded = Desktop.getDesktop().moveToTrash(this)

            publish(DeletionOutcome(this, succeeded))
        } catch (e: SecurityException) {
            publish(DeletionOutcome(this, false))
            return false
        }

        return true
    }

    data class DeletionOutcome(val file: File, val deletionDone: Boolean)
}