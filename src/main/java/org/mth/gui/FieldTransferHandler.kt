package org.mth.gui

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
