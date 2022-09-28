package org.mth.gui

import java.awt.Component
import java.awt.Font
import javax.swing.JCheckBox
import javax.swing.JList
import javax.swing.ListCellRenderer

class MyCellRenderer : JCheckBox(), ListCellRenderer<Any> {

    override fun getListCellRendererComponent(
            list: JList<*>,
            value: Any,
            index: Int,
            isSelected: Boolean,
            hasFocus: Boolean
    ): Component {
        val item = value as JExtensionList.CheckboxListItem

        // configures the selected value of the CheckBox
        setSelected(item.isSelected)

        if (item.isSelected) {
            font = list.font.deriveFont(Font.BOLD)
            foreground = list.foreground
        } else {
            font = list.font
            foreground = list.foreground
        }

        background = list.background
        text = item.extension
        isEnabled = list.isEnabled

        return this
    }

}
