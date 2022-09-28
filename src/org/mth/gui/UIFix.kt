package org.mth.gui

import java.awt.Color
import java.awt.Font
import javax.swing.Icon
import javax.swing.ImageIcon
import javax.swing.UIManager
import javax.swing.plaf.basic.BasicToolTipUI

open class UIFix {

    companion object {
        val ROBOTO_LIGHT: Font = Font.createFont(
            Font.TRUETYPE_FONT,
            this::class.java.classLoader.getResourceAsStream("org/mth/roboto/Roboto-Light.ttf")
        )
        val ROBOTO_REGULAR: Font = Font.createFont(
            Font.TRUETYPE_FONT,
            this::class.java.classLoader.getResourceAsStream("org/mth/roboto/Roboto-Regular.ttf")
        )
    }

    open fun fix() {
        val ui = UIManager.getDefaults()

        ui["defaultFont"] = ROBOTO_REGULAR.deriveFont(12f)

        ui["Label.font"] = ROBOTO_REGULAR.deriveFont(12.0f)
    }

    open fun fixUi2() {
        val ui = UIManager.getDefaults()

        /* TOOLTIP */
        ui["defaultFont"] = ROBOTO_REGULAR.deriveFont(12f)
        println(ui["defaultFont"])
        ui["Tooltip.background"] = Color(23, 37, 92)
        ui["ToolTip.backgroundInactive"] = Color(60, 63, 65)
        ui["ToolTip.foreground"] = Color(230, 230, 230)
        ui["ToolTip.foregroundInactive"] = Color(39, 42, 44)
        ui["ToolTipUI"] = BasicToolTipUI::class.java.canonicalName
        ui["ToolTip.font"] = ROBOTO_REGULAR.deriveFont(12.0f)

        /* LABEL */
        ui["Label.font"] = ROBOTO_REGULAR.deriveFont(13.5f)
        ui["Label.foreground"] = Color(230, 230, 230)

        /* TEXTFIELD */
        ui["TextField.font"] = ui["Label.font"]
        ui["TextField.foreground"] = ui["Label.foreground"]

        ui["TextPane.font"] = ROBOTO_LIGHT.deriveFont(12.0f)

        /* FILECHOOSER */
        ui["FileView.directoryIcon"] = getIcon("material/folder_white_18dp.png")
        ui["FileChooser.newFolderIcon"] = getIcon("material/create_new_folder_white_18dp.png")
        ui["FileChooser.upFolderIcon"] = getIcon("material/folder_open_white_18dp.png")

    }

    open fun fixUi() {
        val ui = UIManager.getDefaults()

        /* TOOLTIP */
        ui["defaultFont"] = ROBOTO_REGULAR.deriveFont(12f)
        println(ui["defaultFont"])
        ui["Tooltip.background"] = Color(23, 37, 92)
        ui["ToolTip.backgroundInactive"] = Color(60, 63, 65)
        ui["ToolTip.foreground"] = Color(230, 230, 230)
        ui["ToolTip.foregroundInactive"] = Color(39, 42, 44)
        ui["ToolTipUI"] = BasicToolTipUI::class.java.canonicalName
        ui["ToolTip.font"] = ROBOTO_REGULAR.deriveFont(12.0f)

        /* LABEL */
        ui["Label.font"] = ROBOTO_REGULAR.deriveFont(13.5f)
        ui["Label.foreground"] = Color(230, 230, 230)

        /* TEXTFIELD */
        ui["TextField.font"] = ui["Label.font"]
        ui["TextField.foreground"] = ui["Label.foreground"]

        ui["TextPane.font"] = ROBOTO_LIGHT.deriveFont(12.0f)

        /* FILECHOOSER */
        ui["FileView.directoryIcon"] = getIcon("material/folder_white_18dp.png")
        ui["FileChooser.newFolderIcon"] = getIcon("material/create_new_folder_white_18dp.png")
        ui["FileChooser.upFolderIcon"] = getIcon("material/folder_open_white_18dp.png")
    }

    private fun getIcon(name: String): Icon = ImageIcon(this.javaClass.classLoader.getResource(name))
}