package org.mth

import java.awt.Desktop
import java.io.File
import java.io.IOException
import javax.swing.SwingWorker


class DeletionTask(val directory: File, val extensions: Set<String>, val toTrash: Boolean) : SwingWorker<Unit, Unit>() {

    var deletionCounter = 0

    override fun doInBackground() {
        Logger.message(formattedTimeStamp(), Logger.timestampStyle)

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