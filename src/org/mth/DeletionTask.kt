package org.mth

import java.awt.Desktop
import java.io.File
import java.io.IOException
import javax.swing.SwingWorker


class DeletionTask(val directory: File, val extensions: Set<String>, val toTrash: Boolean, val recursive: Boolean) :
    SwingWorker<Unit, Unit>() {

    var deletionCounter = 0

    override fun doInBackground() {
        Logger.message(formattedTimeStamp(), Logger.timestampStyle)

        if (recursive)
            recursiveDeletion(directory)
        else {
            val files = directory.listFiles()!!
                .filter { it.isFile }
                .filter { it.extension() in extensions }

            if (files.isEmpty()) {
                Logger.message("Nothing to do")
                return
            }

            if (toTrash)
                moveFilesToTrash(files)
            else
                deleteFiles(files)
        }
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

            directories.forEach { recursiveDeletion(it) }
        }
    }

    private fun deleteFiles(files: List<File>) {
        val bold = Logger.defaultStyle.bold()

        files.forEach {
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

        Logger.message("$deletionCounter files deleted\n", Logger.greenStyle)
    }

    fun File.toTrash(): Boolean {
        val bold = Logger.defaultStyle.bold()

        try {
            val succeeded = Desktop.getDesktop().moveToTrash(this)

            if (succeeded) {
                deletionCounter++
                Logger.append("- ")
                    .append(this.name, bold)
                    .append(" deleted\n")
            } else
                Logger.message("- Cannot delete ${this.name}")
        } catch (e: SecurityException) {
            Logger.message("- Cannot delete ${this.name}")
            return false
        }

        return true
    }

    private fun moveFilesToTrash(files: List<File>) {
        val bold = Logger.defaultStyle.bold()

        files.forEach {
            try {
                val succeeded = Desktop.getDesktop().moveToTrash(it)

                if (succeeded) {
                    deletionCounter++
                    Logger.append("- ")
                        .append(it.name, bold)
                        .append(" deleted\n")
                } else
                    Logger.message("- Cannot delete ${it.name}")
            } catch (e: SecurityException) {
                Logger.message("- Cannot delete ${it.name}")
            }
        }

        Logger.message("$deletionCounter files trashed\n", Logger.greenStyle)
    }

}