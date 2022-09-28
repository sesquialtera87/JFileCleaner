package org.mth.gui

import com.formdev.flatlaf.FlatDarkLaf
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid
import org.kordamp.ikonli.swing.FontIcon
import org.mth.execute
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.io.FileFilter
import java.nio.file.Path
import javax.swing.*
import javax.swing.border.EmptyBorder

class PathBar() : JPanel() {

    init {
        layout = FlowLayout(FlowLayout.LEFT, 0, 0)

        Popup.addPropertyChangeListener(Popup.FOLDER_SELECTION) {
            val path = it.newValue as Path
            update(path)
        }
    }

    private fun constructPath(root: Path, callerButton: JButton): Path {
        var currentPath = root

        for (component in components) {
            val b = component as JButton
            currentPath = currentPath.resolve(b.text)

            if (component == callerButton)
                break
        }

        return currentPath
    }

    fun update(path: Path) {
        removeAll()
        add(JButton(path.root.toString()))

        path.spliterator().forEachRemaining {
            val bt = JButton(it.toString())
            bt.border = EmptyBorder(0, 1, 0, 1)

            bt.addActionListener {
                val constructedPath = constructPath(path.root, bt)
                Popup.populateList(constructedPath)

                if (Popup.needToShow()) {
                    Popup.show(bt.locationOnScreen)
                    println("needed")
                } else {
                    Popup.hidePopup()
                }
            }

            val stringWidth = graphics.getFontMetrics(bt.font).stringWidth(it.toString())
            bt.maximumSize = Dimension(stringWidth + 3, Int.MAX_VALUE)
            bt.preferredSize = Dimension(stringWidth + 3, 24)
            add(bt)

        }

        SwingUtilities.updateComponentTreeUI(parent)
    }

    object Popup : Window(null) {
        const val FOLDER_SELECTION = "folder-selection"

        private val directoryModel = DefaultListModel<String>()
        private val directoryList = JList(directoryModel)
        var path: Path? = null
        var samePath = false

        init {
            minimumSize = Dimension(200, 100)
            layout = BorderLayout()
            add(JScrollPane(directoryList), BorderLayout.CENTER)

            directoryList.cellRenderer = object : DefaultListCellRenderer() {
                override fun getListCellRendererComponent(
                    list: JList<*>?,
                    value: Any?,
                    index: Int,
                    isSelected: Boolean,
                    cellHasFocus: Boolean
                ): Component {
                    val label =
                        super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as JLabel
                    label.icon = FontIcon.of(FontAwesomeSolid.FOLDER, 12, Color.LIGHT_GRAY)
                    return label
                }
            }

            directoryList.addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    if (e.clickCount == 2) {
                        val selectedPath = path?.resolve(directoryList.selectedValue)
                        firePropertyChange(FOLDER_SELECTION, null, selectedPath)

                        // close the popup
                        hidePopup()
                    }
                }
            })
        }

        fun populateList(path: Path) {
            samePath = this.path == path
            this.path = path

            directoryModel.clear()
            directoryModel.addAll(path.toFile().listFiles(FileFilter { it.isDirectory })?.map { it.name })
        }

        fun needToShow() = !directoryModel.isEmpty && !samePath

        fun hidePopup() {
            isVisible = false
            directoryModel.clear()
            path = null
        }

        fun show(point: Point) {
            setLocation(point.x, point.y + 24)
            isVisible = true
        }
    }
}

fun main() {
    execute {
        UIManager.setLookAndFeel(FlatDarkLaf())

        val bar = PathBar()

        JFrame().apply {
            defaultCloseOperation = JFrame.EXIT_ON_CLOSE
            layout = BorderLayout()
            add(bar, BorderLayout.CENTER)
            preferredSize = Dimension(500, 70)
            pack()
            setLocationRelativeTo(null)
            isVisible = true
        }

//        bar.update(File("C:\\Users\\matti\\Documents\\Images\\Jordan Carver\\Christmas in Town"))
        bar.update(File("C:\\Users\\utente\\Documents").toPath())
    }

}