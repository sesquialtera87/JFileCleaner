package org.mth.gui

import org.kordamp.ikonli.fontawesome.FontAwesome
import org.kordamp.ikonli.swing.FontIcon
import org.mth.Application
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.Window
import java.awt.event.ActionEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

class JExtensionList : JList<JExtensionList.CheckboxListItem>() {

    private val extensionInputField = JTextField(" ")
    private val inputPopup: Window by lazy {
        val inputPopup = Window(Application)

        with(inputPopup) {
            layout = BorderLayout()
            minimumSize = Dimension(90, extensionInputField.preferredSize.height)

            add(extensionInputField, BorderLayout.CENTER)
            add(JLabel().apply {
                icon = FontIcon.of(FontAwesome.FILE_TEXT, 16, Color.LIGHT_GRAY)
            }, BorderLayout.WEST)
        }

        inputPopup
    }

    init {
        with(extensionInputField) {
            inputMap.put(KeyStroke.getKeyStroke("ENTER"), "insert-extension")
            inputMap.put(KeyStroke.getKeyStroke("ESCAPE"), "cancel-insertion")

            actionMap.put("insert-extension", InsertExtensionAction())
            actionMap.put("cancel-insertion", CancelInsertionAction())
        }

        actionMap.put("show-input-field", ShowInputField())
        inputMap.put(KeyStroke.getKeyStroke("control N"), "show-input-field")

        model = DefaultListModel()
        cellRenderer = MyCellRenderer()

        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) {
                if (SwingUtilities.isLeftMouseButton(event)) {
                    // Get the closest index of item clicked
                    val index = locationToIndex(event.point)
                    val cellBounds = getCellBounds(index, index)

                    if (event.isShiftDown) {
                        if (cellBounds.contains(event.point)) {
                            // remove a user-defined file extension
                            if (model[index].userDefined)
                                model.remove(index)
                        }
                    } else {
                        if (index >= 0) {
                            // check if the click happened on the cell
                            if (cellBounds.contains(event.point)) {
                                val item = model.getElementAt(index)

                                // Toggle selected state
                                item.isSelected = !item.isSelected

                                // Repaint cell
                                repaint(cellBounds)
                            }
                        }
                    }
                }
            }
        })

        ToolTipManager.sharedInstance().registerComponent(this)
    }


    override fun getModel(): DefaultListModel<CheckboxListItem> =
        if (super.getModel() is DefaultListModel<CheckboxListItem>) super.getModel() as DefaultListModel<CheckboxListItem>
        else DefaultListModel()

    /**
     * Elimina tutti gli elementi dalla lista
     */
    fun clear() {
        model.clear()
    }

    fun checkAll(allChecked: Boolean = true) {
        println(SwingUtilities.isEventDispatchThread())
        model.elements().asSequence().forEach { it.isSelected = allChecked }
        repaint()
    }

    fun getUserDefinedExtensions() = model.toArray().map { it as CheckboxListItem }.filter { it.userDefined }.toSet()

    /**
     * Restituisce gli elementi con la CheckBox selezionata
     *
     * @return
     */
    fun getCheckedExtensions(): Collection<String> = model
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
    fun addExtension(ext: String, fileCount: Int = 0, userDefined: Boolean = false) {
        val ckItem = CheckboxListItem(ext, userDefined)
        ckItem.fileCounter = fileCount
        model.addElement(ckItem)
    }

    fun select(extensions: Collection<String>) {
        val listElements = model
            .toArray()
            .asSequence()
            .map { (it as CheckboxListItem).extension }
            .sorted()
            .toMutableList()

        extensions.map { it.trim() }
            // .sorted()
            .forEach {
                val position = listElements.binarySearch(it, { s1, s2 -> s1.compareTo(s2) })
                model[position].isSelected = true
            }
    }


    override fun getToolTipText(event: MouseEvent): String {
        /* indice della JList associato alla corrente posizione del mouse */
        val listIndex = locationToIndex(event.point)

        return if (listIndex >= 0)
            "${model[listIndex].fileCounter} files"
        else ""
    }


    data class CheckboxListItem(val extension: String, val userDefined: Boolean = false) {
        var isSelected = false
        var fileCounter = 0

        override fun equals(other: Any?): Boolean {
            return if (other == null) {
                false
            } else {
                if (other is CheckboxListItem) {
                    other.extension.equals(extension, true)
                } else
                    false
            }
        }

        override fun hashCode(): Int {
            var result = extension.hashCode()
            result = 31 * result + userDefined.hashCode()
            result = 31 * result + isSelected.hashCode()
            result = 31 * result + fileCounter
            return result
        }
    }

    /**
     * The action fired by pressing ENTER on the [extensionInputField]
     */
    inner class InsertExtensionAction : AbstractAction() {
        override fun actionPerformed(e: ActionEvent?) {
            val extension = extensionInputField.text.trim()

            if (!model.contains(CheckboxListItem(extension)))
                addExtension(extension, userDefined = true)

            inputPopup.isVisible = false
        }
    }

    inner class CancelInsertionAction : AbstractAction() {
        override fun actionPerformed(e: ActionEvent?) {
            inputPopup.isVisible = false
        }
    }

    inner class ShowInputField : AbstractAction() {
        override fun actionPerformed(e: ActionEvent?) {
            val point = this@JExtensionList.locationOnScreen

            with(inputPopup) {
                isVisible = true
                setLocation(point.x + width / 2, point.y + 40)
                toFront()
            }

            extensionInputField.text = ""
            extensionInputField.requestFocusInWindow()
        }
    }
}
