package org.mth.gui

import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.DefaultListModel
import javax.swing.JList
import javax.swing.SwingUtilities
import javax.swing.ToolTipManager

class JExtensionList : JList<JExtensionList.CheckboxListItem>() {

    init {
        model = DefaultListModel()
        cellRenderer = MyCellRenderer()

        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) {
                if (SwingUtilities.isLeftMouseButton(event)) {
                    // Get index of item clicked
                    val index = locationToIndex(event.point)

                    if (index >= 0) {
                        val item = model!!.getElementAt(index)

                        // Toggle selected state
                        item.isSelected = !item.isSelected

                        // Repaint cell
                        repaint(getCellBounds(index, index))
                    }
                }
            }
        })

        ToolTipManager.sharedInstance().registerComponent(this)
    }


    override fun getModel(): DefaultListModel<CheckboxListItem>? {
        return if (super.getModel() is DefaultListModel)
            super.getModel() as DefaultListModel<CheckboxListItem>
        else null
    }

    /**
     * Elimina tutti gli elementi dalla lista
     */
    fun clear() {
        model!!.clear()
    }

    fun checkAll(allChecked: Boolean = true) {
        model!!.elements().asSequence().forEach { it.isSelected = allChecked }
        repaint()
    }


    /**
     * Restituisce gli elementi con la CheckBox selezionata
     *
     * @return
     */
    fun getCheckedExtensions(): Collection<String> = model!!
        .elements()
        .asSequence()
        .filter { it.isSelected }
        .map { it.extension }
        .toList()

    /**
     * Aggiunge un elemento alla lista
     *
     * @param ext
     */
    fun addExtension(ext: String, fileCount: Int = 0) {
        val ckItem = CheckboxListItem(ext)
        ckItem.fileCounter = fileCount
        model!!.addElement(ckItem)
    }

    fun select(extensions: Collection<String>) {
        val listElements = model!!
            .toArray()
            .asSequence()
            .map { (it as CheckboxListItem).extension }
            .sorted()
            .toMutableList()

        extensions.map { it.trim() }
            // .sorted()
            .forEach {
                val position = listElements.binarySearch(it, { s1, s2 -> s1.compareTo(s2) })
                model!![position].isSelected = true
            }
    }


    override fun getToolTipText(event: MouseEvent): String {
        /* indice della JList associato alla corrente posizione del mouse */
        val listIndex = locationToIndex(event.point)

        return if (listIndex >= 0)
            "${model!![listIndex].fileCounter} files"
        else ""
    }


    data class CheckboxListItem(val extension: String) {
        var isSelected = false
        var fileCounter = 0
    }
}
