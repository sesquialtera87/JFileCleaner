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

import com.formdev.flatlaf.FlatLaf
import com.formdev.flatlaf.FlatLightLaf
import com.formdev.flatlaf.extras.components.FlatTextField
import com.formdev.flatlaf.util.FontUtils
import net.miginfocom.swing.MigLayout
import org.apache.ibatis.io.Resources
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid
import org.kordamp.ikonli.swing.FontIcon
import org.mth.cleaner.ApplicationContext
import org.mth.cleaner.ApplicationContext.*
import org.mth.cleaner.ui.FlatLightUI.Companion.installFont
import org.mth.execute
import org.mth.getFormattedSize
import org.mth.swing.ToggleButton
import org.tinylog.Logger
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.io.File
import java.nio.file.Files
import java.util.*
import java.util.function.Consumer
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.filechooser.FileSystemView
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableCellRenderer
import kotlin.io.path.absolutePathString

@Suppress("JavaIoSerializableObjectMustHaveReadResolve")
class FlatLightUI : JFrame(), PropertyChangeListener {

    private data class ExtensionInfo(
        val extension: String,
        val fileCount: Int = 0,
        var selected: Boolean = false,
        val size: Long,
    )

    private inner class CellRenderer : DefaultTableCellRenderer() {
        val check = JCheckBox().apply {
            horizontalAlignment = CENTER
        }

        override fun getTableCellRendererComponent(
            table: JTable,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int,
        ): Component {
            val c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column) as JLabel
            c.border = EmptyBorder(0, 3, 0, 0)

            if (table.convertColumnIndexToModel(column) == 0) {
                check.isSelected = value as Boolean

                if (isSelected)
                    check.background = UIManager.getColor("Table.selectionBackground")
                else
                    check.background = UIManager.getColor("Table.background")

                return check
            } else if (table.convertColumnIndexToModel(column) >= 2) {
                if (isSelected)
                    c.foreground = UIManager.getColor("Table.selectionForeground")
                else
                    c.foreground = UIManager.getColor("Table.foreground")
            } else {
                c.foreground = Color.BLACK
            }

            if (table.convertColumnIndexToModel(column) == 1) {
                c.iconTextGap = 8
                c.icon = iconCache[value]
            }

            return c
        }
    }

    private object CheckBoxHeaderRenderer : TableCellRenderer {
        val checkBox = JCheckBox().apply {
            horizontalAlignment = SwingConstants.CENTER
            background = UIManager.getColor("TableHeader.background")
        }

        var table: JTable? = null

        override fun getTableCellRendererComponent(
            table: JTable,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int,
        ): Component {
            CheckBoxHeaderRenderer.table = table
            return checkBox
        }

        fun syncWithSelection() {
            checkBox.isSelected = ExtensionModel.rowCount > 0 && ExtensionModel.selectionCount() == ExtensionModel.rowCount
            table?.tableHeader?.repaint()
        }
    }

    private object ExtensionModel : AbstractTableModel() {
        private var data = mutableListOf<ExtensionInfo>()

        fun selectAll(selected: Boolean) {
            data.forEachIndexed { n, info ->
                info.selected = selected
                fireTableCellUpdated(n, 0)
            }
        }

        fun toggleSelection(row: Int) {
            data[row].selected = !(data[row].selected)
            fireTableCellUpdated(row, 0)
        }

        fun setData(data: Collection<ExtensionInfo>) {
            this.data.clear()
            this.data.addAll(data)
            fireTableDataChanged()
        }

        fun getAllExtensions(): Collection<String> = data.map { it.extension }

        fun selectionCount(): Int = data.count { it.selected }

        fun selectedFileCount(): Int = data.filter { it.selected }.sumOf { it.fileCount }

        fun selectedTotalFileSize(): Long = data.filter { it.selected }.sumOf { it.size }

        fun getSelectedExtensions(): Set<String> = data.filter { it.selected }
            .map { it.extension.lowercase() }
            .toSet()

        override fun isCellEditable(rowIndex: Int, columnIndex: Int) = false

        override fun getRowCount(): Int = data.size

        override fun getColumnName(column: Int): String {
            return arrayOf("", "EXTENSION", "FILES", "DIMENSION")[column]
        }

        override fun getColumnCount(): Int = 4

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
            return when (columnIndex) {
                0 -> data[rowIndex].selected
                1 -> data[rowIndex].extension
                2 -> data[rowIndex].fileCount
                3 -> data[rowIndex].size.getFormattedSize()
                else -> ""
            }
        }
    }

    private val subfolderStatistics: MutableMap<String, Int> = mutableMapOf()

    private val iconCache = mutableMapOf<String, Icon>()

    private var currentDirectory = File(System.getProperty("user.home"))

    private val fileChooser = JFileChooser().apply {
        fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        isMultiSelectionEnabled = false
    }

    private val trashToggleButton = ToggleButton().apply {
        foreground = UIManager.getColor("primaryColor")
        preferredSize = Dimension(50, 30)
        minimumSize = preferredSize
        isEnabled = Desktop.getDesktop().isSupported(Desktop.Action.MOVE_TO_TRASH)
    }

    private val recursionToggleButton = ToggleButton().apply {
        foreground = UIManager.getColor("primaryColor")
        preferredSize = trashToggleButton.preferredSize
    }

    private val clean = JButton("CLEAN").apply {
        font = UIManager.getFont("h3.font")
        setMnemonic('C')
        putClientProperty("JComponent.roundRect", true)
        addActionListener { clean() }
    }

    private val browseButton = JButton(i18nString("browseButton.text")).apply {
        font = UIManager.getFont("h4.font")
        setMnemonic('F')
        addActionListener { browseDirectory() }
        putClientProperty("JComponent.roundRect", true)
    }

    private val directoryField = FlatTextField().apply {
        leadingIcon = FontIcon.of(FontAwesomeSolid.FOLDER, 16, UIManager.getColor("primaryColor"))
        addActionListener {
            chronologyPopup.dispose()

            val path = File(text)
            if (path.exists() && path != currentDirectory)
                updateDirectory(path)
            else
                UIManager.getLookAndFeel().provideErrorFeedback(this)
        }
        transferHandler = FieldTransferHandler { path ->
            if (path.exists() && path != currentDirectory)
                updateDirectory(path)
            else
                UIManager.getLookAndFeel().provideErrorFeedback(this)
        }
    }

    private val table: JTable = JTable(ExtensionModel).apply {
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (SwingUtilities.isLeftMouseButton(e))
                    if (e.clickCount == 1) {
                        if (table.convertColumnIndexToModel(table.columnAtPoint(e.point)) == 0) {
                            execute {
                                ExtensionModel.toggleSelection(table.rowAtPoint(e.point))
                                CheckBoxHeaderRenderer.syncWithSelection()
                                syncFileCountLabel()
                                syncFileSizeLabel()
                            }
                        }
                    }
            }
        })
        columnModel.getColumn(0).maxWidth = 40
        columnModel.getColumn(0).minWidth = 40
        columnModel.getColumn(0).cellRenderer = CellRenderer()
        columnModel.getColumn(0).headerRenderer = CheckBoxHeaderRenderer

        for (i in 1..columnCount - 1) {
            columnModel.getColumn(i).cellRenderer = CellRenderer()
            columnModel.getColumn(i).headerRenderer = object : DefaultTableCellRenderer() {

                override fun getTableCellRendererComponent(
                    table: JTable?,
                    value: Any?,
                    isSelected: Boolean,
                    hasFocus: Boolean,
                    row: Int,
                    column: Int,
                ): Component {
                    return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column).apply {
                        horizontalTextPosition = LEFT
                    }
                }
            }
        }

        tableHeader.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (SwingUtilities.isLeftMouseButton(e))
                    if (e.clickCount == 1) {
                        if (tableHeader.columnAtPoint(e.point) == 0) {
                            CheckBoxHeaderRenderer.checkBox.isSelected = !CheckBoxHeaderRenderer.checkBox.isSelected
                            ExtensionModel.selectAll(CheckBoxHeaderRenderer.checkBox.isSelected)
                            syncFileSizeLabel()
                            syncFileCountLabel()
                        }
                    }
            }
        })
    }

    private val chronologyPopup: ChronologyPopup = ChronologyPopup(this, directoryField).apply {
        onItemClick = Consumer {
            updateDirectory(it.toFile())
            chronologyPopup.dispose()
        }
    }

    private val scroll = JScrollPane(table)

    private val optionPanel = JPanel().apply {
        layout = MigLayout("", "[grow][grow]") // NON-NLS
        isOpaque = false

        JPanel(FlowLayout(FlowLayout.LEADING)).apply {
            add(JLabel(i18nString("trashOption.text")).apply { font = UIManager.getFont("medium.font") })
            add(trashToggleButton)
            isOpaque = false
        }.let { this.add(it) }

        JPanel(FlowLayout(FlowLayout.LEADING)).apply {
            add(JLabel(i18nString("recursionOption.text")).apply { font = UIManager.getFont("medium.font") })
            add(recursionToggleButton)
            isOpaque = false
        }.let { this.add(it, "wrap") } // NON-NLS
    }

    private val fileCountLabel = JLabel("0")

    private val fileSizeLabel = JLabel("0 bytes") // NON-NLS

    private val mainScene = object : Drawer.Scene() {
        init {
            background = UIManager.getColor("sceneColor")
            layout = MigLayout("insets 10 20 10 20", "[fill, grow][fill]") // NON-NLS

            val fileCountLabelCaption = JLabel().apply {
                text = i18nString("fileCountLabel.text")
                labelFor = fileCountLabel
                icon = getSvgIcon("check.svg", 24, 24, Color.WHITE, Color.decode("#E74C3C")) // NON-NLS
                iconTextGap = 7
            }

            val fileSizeLabelCaption = JLabel().apply {
                text = i18nString("fileSizeLabel.text")
                labelFor = fileSizeLabel
                icon = getSvgIcon("file-size.svg", 24, 24, Color.WHITE, Color.decode("#27AE60")) // NON-NLS
                iconTextGap = 7
            }

            val statPanel = JPanel().apply {
                isOpaque = false
                layout = MigLayout("", "[]5[30][]5[]")
                add(fileCountLabelCaption)
                add(fileCountLabel)
                add(fileSizeLabelCaption, "gapx 8") // NON-NLS
                add(fileSizeLabel)
            }

            add(directoryField, "gapy 8, growx") // NON-NLS
            add(browseButton, "wrap") // NON-NLS
            add(statPanel, "gapy 8, span 2, growx, wrap") // NON-NLS
            add(scroll, "gapy 8, span 2, growx, growy, wrap") // NON-NLS
            add(optionPanel, "gapy 8, span2, wrap") // NON-NLS
            add(clean, "align center, span 2, wrap") // NON-NLS
        }

        @Suppress("HardCodedStringLiteral")
        override fun getPreferences(): Properties {
            return Properties().apply {
                setProperty("last-folder", currentDirectory.toPath().normalize().absolutePathString())
                setProperty("use-trash", trashToggleButton.isSelected.toString())
            }
        }

        override fun injectPreferences(properties: Properties) {
            execute {
                trashToggleButton.isSelected = properties.getProperty("use-trash", "false").toBoolean()
                val lastFolder = File(properties.getProperty("last-folder", System.getProperty("user.home")))
                updateDirectory(lastFolder)
            }
        }

        override fun onShow() {
            updateDirectory(currentDirectory)
        }
    }

    private val scenePanel = JPanel(BorderLayout()).apply {
        isOpaque = true
        background = UIManager.getColor("sceneBackground")
    }

    private val drawer = Drawer(scenePanel).apply {
        addScene(i18nString("scene.clean.title"), getSvgIcon("delete.svg", 22, 22), mainScene)
        addScene(i18nString("scene.history.title"), getSvgIcon("history.svg", 22, 22, Color.WHITE, UIManager.getColor("primaryForeground")), HistoryPanel())
    }

    init {
        addPropertyChangeListener(this)

        add(scenePanel, BorderLayout.CENTER)
        add(drawer, BorderLayout.WEST)

        title = i18nString("app.name")
        defaultCloseOperation = EXIT_ON_CLOSE
        preferredSize = Dimension(720, 480)
    }

    fun injectPreferences(properties: Properties) {
        drawer.getScenes().forEach { it.injectPreferences(properties) }
    }

    fun getPreferences() = Properties().apply {
        drawer.getScenes().forEach { putAll(it.getPreferences()) }

        setProperty("frame.width", this@FlatLightUI.width.toString())
        setProperty("frame.height", this@FlatLightUI.height.toString())
    }

    private fun syncFileCountLabel() {
        Logger.debug { "File sum: " + ExtensionModel.selectedFileCount() } // NON-NLS

        if (recursionToggleButton.isSelected) {
            val selectedExtensions = ExtensionModel.getSelectedExtensions()
            var tot = ExtensionModel.selectedFileCount() + subfolderStatistics.filter { it.key.lowercase() in selectedExtensions }.map { it.value }.first()

            fileCountLabel.text = tot.toString()
        } else
            fileCountLabel.text = ExtensionModel.selectedFileCount().toString()
    }

    private fun syncFileSizeLabel() {
        Logger.debug { "File size sum: " + ExtensionModel.selectedTotalFileSize() } // NON-NLS
        fileSizeLabel.text = ExtensionModel.selectedTotalFileSize().getFormattedSize()
    }

    private fun clean() {
        if (ExtensionModel.selectionCount() == 0)
            return

        val choice = JOptionPane.showConfirmDialog(
            this,
            i18nString("confirmDeletion.text"),
            i18nString("app.name"),
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        )

        if (choice != JOptionPane.YES_OPTION)
            return

        val dialog = ProgressPopup()
        val job = DeletionJob(currentDirectory, ExtensionModel.getSelectedExtensions(), trashToggleButton.isSelected, recursionToggleButton.isSelected, dialog)

        SwingUtilities.invokeLater {
            dialog.addJob(job)
            dialog.setLocationRelativeTo(this)
            dialog.isVisible = true
        }
    }

    private fun browseDirectory() {
        fileChooser.currentDirectory = currentDirectory
        val result = fileChooser.showOpenDialog(this)

        if (result == JFileChooser.APPROVE_OPTION) {
            if (fileChooser.selectedFile != currentDirectory) {
                if (!(fileChooser.selectedFile.exists()))
                    UIManager.getLookAndFeel().provideErrorFeedback(directoryField)
                else
                    updateDirectory(fileChooser.selectedFile)
            }
        }
    }

    private fun updateDirectory(directory: File) {
        addToChronology(directory.toPath())

        currentDirectory = directory
        directoryField.text = currentDirectory.toPath().normalize().absolutePathString()

        /**
         * Check if exists the icon associated to the file extension. If not, get the icon using [FileSystemView] and cache it for later use.
         */
        fun File.checkExtensionIcon(): Pair<File, String> {
            val ext = extension.uppercase()

            if (ext !in iconCache)
                iconCache[ext] = FileSystemView.getFileSystemView().getSystemIcon(this, 18, 18)

            return Pair(this, ext)
        }

        try {
            val files: Array<out File>? = currentDirectory.listFiles()

            if (files != null) {
                files.filter { it.isFile }
                    .map { it.checkExtensionIcon() }
                    .sortedBy { it.second }
                    .groupingBy { it.second }
                    .fold(Pair(0L, 0)) { tot, e -> Pair(tot.first + Files.size(e.first.toPath()), tot.second + 1) }
                    .map { ExtensionInfo(it.key, it.value.second, false, it.value.first) }
                    .run {
                        execute {
                            ExtensionModel.setData(this)
                            CheckBoxHeaderRenderer.syncWithSelection()
                        }
                    }

                if (recursionToggleButton.isSelected) {
                    val task = SubfolderVisitor(currentDirectory.toPath(), ExtensionModel.getAllExtensions())
                    task.addPropertyChangeListener {
                        if (it.propertyName == "state" && it.newValue == SwingWorker.StateValue.DONE) {
                            subfolderStatistics.clear()
                            subfolderStatistics.putAll(task.get())
                        }
                    }
                    task.execute()
                }
            }
        } catch (e: SecurityException) {
            Logger.error(e)
        }
    }

    override fun propertyChange(evt: PropertyChangeEvent) {
        if (evt.propertyName == "job-done") {
            updateDirectory(currentDirectory)
        }
    }

    companion object {

        @Suppress("HardCodedStringLiteral")
        @JvmStatic
        fun installFont() {
            GraphicsEnvironment.getLocalGraphicsEnvironment().run {
                arrayOf(
                    "Outfit-Regular",
                    "Outfit-Bold",
                    "Outfit-SemiBold",
                    "Outfit-Light",
                    "Outfit-Medium",
                ).forEach {
                    val url = Resources.getResourceURL(ApplicationContext::class.java.classLoader, "org/mth/font/$it.ttf")
                    FontUtils.installFont(url)
                }

            }
        }
    }
}

fun main() {
    installFont()

    FlatLaf.registerCustomDefaultsSource("org.mth.cleaner")
    FlatLightLaf.setup()

    FlatLightUI().run {
        pack()
        isVisible = true
        setLocationRelativeTo(null)
    }
}