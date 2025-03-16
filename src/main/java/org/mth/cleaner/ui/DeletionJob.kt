/*
 * Copyright (c) 2025 Mattia Marelli
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package org.mth.cleaner.ui

import org.mth.cleaner.ApplicationContext
import org.mth.cleaner.ApplicationContext.i18nString
import org.mth.extension
import org.mth.sqlite.DeletionRecord
import org.mth.sqlite.openSession
import org.tinylog.Logger
import java.awt.Desktop
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.time.Instant
import javax.swing.SwingWorker

class DeletionJob(
    private val directory: File,
    private val extensions: Set<String>,
    private val toTrash: Boolean,
    private val recursive: Boolean,
    private val dialog: ProgressPopup,
) :
    SwingWorker<Unit, Void>() {

    private val instant: Instant = Instant.now()
    private val info = mutableListOf<DeletionRecord>()

    override fun doInBackground() {
        Logger.debug { "doInBackground" }

        recursiveDeletion(directory)

        message(i18nString("job.deletionCompleted").format(info.size))
        Thread.sleep(1400)

        if (info.isNotEmpty()) {
            with(openSession()) {
                info.forEach { insert("insert-deletion-record", it) } // NON-NLS
                commit(true)
                close()
            }

            ApplicationContext.fire("job-done", '1', '0')
        }
    }

    private fun message(text: String) = org.mth.execute { dialog.message(text) }

    private fun recursiveDeletion(path: File) {
        val fileCollection = path.listFiles()

        Logger.debug { fileCollection.contentToString() }

        if (fileCollection != null) {
            val (files, directories) = fileCollection.partition { it.isFile }

            if (toTrash)
                files.filter { it.extension().lowercase() in extensions }
                    .forEach { it.toTrash() }
            else
                deleteFiles(files.filter { it.extension().lowercase() in extensions })

            // proceed in recursion
            if (recursive)
                for (dir in directories) {
                    if (isCancelled) {
                        Logger.info { "Task canceled" } // NON-NLS
                        break
                    }

                    recursiveDeletion(dir)
                }
        }
    }

    private fun deleteFiles(files: List<File>) {
        Logger.debug { "deleteFiles" }

        for (file in files) {
            if (isCancelled) {
                Logger.info { "Task canceled" } // NON-NLS
                break
            }

            try {
                val record = DeletionRecord(file.extension, file.absolutePath, instant.toEpochMilli().toString(), 0, Files.size(file.toPath()))

                message(i18nString("job.deleting").format(file.relativeTo(directory)))

                val succeeded = file.delete()

                if (succeeded) {
                    Logger.debug { "Deleting: $file" } // NON-NLS
                    info.add(record)
                } else
                    Logger.warn { "Cannot delete: $file" } // NON-NLS

                Thread.sleep(100)
            } catch (e: IOException) {
                Logger.trace(e)
                Logger.error { "EXCEPTION! Cannot delete: $file" } // NON-NLS
            } catch (e: SecurityException) {
                Logger.trace(e)
                Logger.error { "EXCEPTION! Cannot delete: $file" } // NON-NLS
            }
        }
    }


    /**
     * Move a file to the system trash
     */
    private fun File.toTrash(): Boolean {
        try {
            message(i18nString("job.deleting").format(this.relativeTo(directory)))

            val record = DeletionRecord(this.extension, this.absolutePath, instant.toEpochMilli().toString(), 1, Files.size(this.toPath()))
            val succeeded = Desktop.getDesktop().moveToTrash(this)

            if (succeeded)
                info.add(record)

            return succeeded
        } catch (e: SecurityException) {
            Logger.error(e)
            return false
        }
    }
}