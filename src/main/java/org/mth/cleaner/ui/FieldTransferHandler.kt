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

import org.tinylog.Logger
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.File
import java.io.IOException
import javax.swing.TransferHandler
import javax.swing.UIManager

/**
 *
 * @author mattia
 */
class FieldTransferHandler(
    /**
     * Executed when the drop is accepted.
     * Used for customise the action after drop
     */
    var dropFunction: (File) -> Unit = {},
) : TransferHandler() {

    /**
     * accept only file transfers
     */
    override fun canImport(support: TransferSupport): Boolean =
        support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)


    override fun importData(support: TransferSupport): Boolean {
        try {
            if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                val data = support.transferable.getTransferData(DataFlavor.javaFileListFlavor) as Collection<*>
                val directory = data.map { it as File }.firstOrNull { it.isDirectory }

                if (directory == null)
                    return false
                else
                    dropFunction.invoke(directory)
            } else if (support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                val data = support.transferable.getTransferData(DataFlavor.stringFlavor) as String
                val file = File(data)

                if (file.exists() && file.isDirectory)
                    dropFunction.invoke(file)
                else {
                    UIManager.getLookAndFeel().provideErrorFeedback(support.component)
                    return false
                }
            }
        } catch (ex: UnsupportedFlavorException) {
            Logger.error(ex)
        } catch (ex: IOException) {
            Logger.error(ex)
        }

        return true
    }
}