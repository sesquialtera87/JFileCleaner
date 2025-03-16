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

package org.mth

import org.tinylog.Logger
import java.io.File
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.SwingUtilities
import kotlin.io.path.Path


fun File.extension() = if (this.isDirectory) ""
else this.name.substring(this.name.lastIndexOf('.') + 1).lowercase()

/**
 * Shortcut for [SwingUtilities.invokeLater]
 */
fun execute(runnable: Runnable) {
    if (SwingUtilities.isEventDispatchThread())
        runnable.run()
    else
        SwingUtilities.invokeLater { runnable.run() }
}


@Suppress("HardCodedStringLiteral", "KotlinConstantConditions")
fun Long.getFormattedSize(): String {
    if (this < 1024)
        return "$this bytes"

    var S = this / 1024

    if (S < 1024)
        return "$S KB"

    S = S / 1024

    if (S < 1024)
        return "$S MB"

    S = S / 1024

    return "$S GB"
}

@Suppress("HardCodedStringLiteral")
fun createRandomFiles(
    n: Int = 10,
    dir: Path = Path(System.getProperty("user.dir"), "file_test"),
    subfolderCount: Int = 1,
) {
    val extensions = listOf("fd", "ggdf", "j9", "ppo")
    var subfolders = Array<Path>(subfolderCount) { dir.resolve("subfolder_$it") }

    subfolders.forEach {
        try {
            Files.createDirectory(it)
        } catch (e: FileAlreadyExistsException) {
            Logger.error(e)
        }
    }

    (1..n).forEach { i ->
        try {
            Files.createFile(dir.resolve("file_${System.currentTimeMillis()}." + extensions.random()))
        } catch (_: FileAlreadyExistsException) {

        }
    }

    subfolders.forEach {
        (1..n).forEach { i ->
            try {
                Files.createFile(it.resolve("file_${System.currentTimeMillis()}." + extensions.random()))
            } catch (_: FileAlreadyExistsException) {

            }
        }
    }
}
