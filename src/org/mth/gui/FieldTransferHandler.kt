/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.mth.gui

import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.File
import java.io.IOException
import javax.swing.TransferHandler

/**
 *
 * @author mattia
 */
class FieldTransferHandler : TransferHandler() {

    /**
     * Executed when the drop is accepted.
     * Used for customise the action after drop
     */
    var dropFunction: (File) -> Unit = {}


    /**
     * accept only file transfers
     */
    override fun canImport(support: TransferSupport): Boolean =
            support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)


    override fun importData(support: TransferSupport): Boolean {
        try {
            val data = support.transferable.getTransferData(DataFlavor.javaFileListFlavor) as Collection<*>

            // ammette solo 1 file...
            if (data.size > 1) return false

            val file = data.iterator().next() as File

            if (file.isDirectory) {
                dropFunction.invoke(file)
            } else {
                return false
            }
        } catch (ex: UnsupportedFlavorException) {
            println(ex)
        } catch (ex: IOException) {
            println(ex)
        }

        return true
    }
}
