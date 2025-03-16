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

import com.formdev.flatlaf.FlatClientProperties
import net.miginfocom.swing.MigLayout
import org.jetbrains.annotations.NonNls
import org.kordamp.ikonli.fontawesome5.FontAwesomeRegular
import org.kordamp.ikonli.swing.FontIcon
import org.mth.cleaner.ApplicationContext.*
import org.mth.getFormattedSize
import org.mth.sqlite.DeletionRecord
import org.mth.sqlite.openSession
import org.tinylog.Logger
import java.awt.Color
import java.awt.Component
import java.awt.GridLayout
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import java.awt.event.MouseEvent
import java.time.Instant
import java.time.Month
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*
import javax.swing.*
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer
import kotlin.io.path.Path
import kotlin.io.path.name
import kotlin.system.measureTimeMillis

class HistoryPanel : Drawer.Scene(), ItemListener {

    /**
     * The model of the history table
     */
    private val myTableModel = TableModel()

    /**
     * The [JComboBox] for year filtering
     */
    private val year = JComboBox<Int>().apply {
        addItemListener(this@HistoryPanel)
        putClientProperty("JComponent.roundRect", true)
        putClientProperty("Component.arc", 10)
        putClientProperty(FlatClientProperties.STYLE, "background: #4d55cc; buttonBackground: #4d55cc;")
    }

    private val month = JComboBox<Int>().apply {
        renderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(
                list: JList<*>,
                value: Any?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean,
            ): Component {
                val lbl = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as JLabel

                if (value != null)
                    if (value is Int)
                        lbl.text = Month.of(value).getDisplayName(TextStyle.FULL, Locale.getDefault())

                return lbl
            }
        }
        addItemListener(this@HistoryPanel)
        putClientProperty("JComponent.roundRect", true)
        putClientProperty(FlatClientProperties.STYLE, "background: \$green; buttonBackground: \$green;")
    }

    private val day = JComboBox<Int>().apply {
        renderer = object : DefaultListCellRenderer() {

            val ALL = i18nString("history.dayBox.all")

            override fun getListCellRendererComponent(
                list: JList<*>,
                value: Any?,
                index: Int,
                isSelected: Boolean,
                cellHasFocus: Boolean,
            ): Component {
                val lbl = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as JLabel

                if (value == Int.MIN_VALUE)
                    lbl.text = ALL

                return lbl
            }
        }
        addItemListener(this@HistoryPanel)
        putClientProperty("JComponent.roundRect", true)
        putClientProperty(FlatClientProperties.STYLE, "background: #f96e2a; buttonBackground: #f96e2a;")
    }

    private val table: JTable = object : JTable(myTableModel) {
        init {
            selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION

            for (i in 0 until columnModel.columnCount) {
                columnModel.getColumn(i).headerRenderer = object : DefaultTableCellRenderer() {
                    override fun getTableCellRendererComponent(
                        table: JTable,
                        value: Any?,
                        isSelected: Boolean,
                        hasFocus: Boolean,
                        row: Int,
                        column: Int,
                    ): Component {
                        val lbl = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column) as JLabel
                        lbl.horizontalAlignment = LEFT
                        return lbl
                    }
                }
            }
        }

        override fun getToolTipText(event: MouseEvent): String? {
            val row = table.rowAtPoint(event.point)
            val col = table.columnAtPoint(event.point)

            return if (table.convertColumnIndexToModel(col) == 1)
                myTableModel.getRecord(row).path
            else
                super.getToolTipText(event)
        }
    }

    @NonNls
    private val extensionTile = Tile(getSvgIcon("file-extension.svg", 32, 32), "0", i18nString("history.tile.extension"), Color.decode("#27AE60"), width = 160)

    @NonNls
    private val fileCountTile = Tile(getSvgIcon("file-set.svg", 32, 32), "0", i18nString("history.tile.files"), Color.decode("#9B59B6"), width = 160)

    @NonNls
    private val fileSizeTile = Tile(getSvgIcon("file-size.svg", 32, 32), "0 bytes", i18nString("history.tile.fileSize"), Color.decode("#E67E22"), width = 160)

    /**
     * Shows the statistic about the current view
     */
    private val tilePanel = JPanel().apply {
        layout = GridLayout(1, 3, 8, 0)
        add(fileCountTile)
        add(fileSizeTile)
        add(extensionTile)
    }

    init {
        isOpaque
        layout = MigLayout("insets 20 20 10 20", "[grow, fill]") // NON-NLS

        val clearHistoryButton = JButton().apply {
            text = i18nString("history.clearButton.text")
            addActionListener { clearHistory() }
        }

        val filterPanel = JPanel().apply {
            isOpaque = false
            layout = MigLayout("", "[][][][fill, grow][right]") // NON-NLS

            add(JLabel(i18nString("history.yearLabel.text")).apply { horizontalAlignment = SwingConstants.CENTER })
            add(JLabel(i18nString("history.monthLabel.text")).apply { horizontalAlignment = SwingConstants.CENTER })
            add(JLabel(i18nString("history.dayLabel.text")).apply { horizontalAlignment = SwingConstants.CENTER }, "wrap") // NON-NLS

            add(year)
            add(month)
            add(day) // NON-NLS
            add(clearHistoryButton, "cell 4 1") // NON-NLS
        }

        add(tilePanel, "span, growx, wrap") // NON-NLS
        add(JScrollPane(table), "gapy 15, span 4, growx, growy, wrap") // NON-NLS
        add(filterPanel, "span") // NON-NLS

        table.columnModel.getColumn(4).cellRenderer = TrashCellRenderer()
    }

    fun reload() {
        jobExecutor.submit(HistoryLoader())
    }

    @Suppress("HardCodedStringLiteral")
    override fun getPreferences(): Properties {
        val properties = Properties()

        for (i in 0 until table.columnCount) {
            properties.setProperty("history.table.column.$i.name", table.model.getColumnName(i))
            properties.setProperty("history.table.column.$i.width", table.columnModel.getColumn(i).width.toString())
            properties.setProperty("history.table.column.$i.preferred-width", table.columnModel.getColumn(i).preferredWidth.toString())
        }

        return properties
    }

    @Suppress("HardCodedStringLiteral")
    override fun injectPreferences(properties: Properties) {
        for (i in 0 until table.columnCount) {
            if (properties.containsKey("history.table.column.$i.preferred-width"))
                table.columnModel.getColumn(i).preferredWidth = properties.getProperty("history.table.column.$i.preferred-width").toInt()

            if (properties.containsKey("history.table.column.$i.width"))
                table.columnModel.getColumn(i).width = properties.getProperty("history.table.column.$i.width").toInt()
        }
    }

    override fun onHide() {
        myTableModel.clear()
    }

    override fun onShow() {
        org.mth.execute {
            updateYearBox()
            updateMonthBox()
            updateDayBox()
        }

        reload()
    }

    @Suppress("HardCodedStringLiteral", "RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    private fun clearHistory() {
        if (year.selectedItem == null || month.selectedItem == null)
            return

        val choice = JOptionPane.showConfirmDialog(
            FRAME,
            "Are you sure to clear the current history view?",
            i18nString("app.name"),
            JOptionPane.YES_NO_OPTION,
            JOptionPane.QUESTION_MESSAGE
        )

        if (choice != JOptionPane.YES_OPTION) return

        with(openSession()) {
            if (day.selectedItem != Int.MIN_VALUE) {
                val params = mapOf(
                    "year" to year.selectedItem.toString(),
                    "month" to "%02d".format(month.selectedItem), // strftime function of SQLITE get always two digits for month and day...
                    "day" to "%02d".format(day.selectedItem),
                )
                val rows = delete("delete-by-day", params)

                Logger.debug { "Deletion parameters: $params" }
                Logger.debug { "Deleted lines: $rows" }
            } else {
                val params = mapOf(
                    "year" to year.selectedItem.toString(),
                    "month" to "%02d".format(month.selectedItem),
                )
                val rows = delete("delete-by-month", params)

                Logger.debug { "Deletion parameters: $params" }
                Logger.debug { "Deleted lines: $rows" }
            }

            commit()
            close()

            // todo maintain the current selected year, month and select the nearest day (or ALL)
            // update the view
            onShow()
        }
    }

    @Suppress("HardCodedStringLiteral")
    private fun updateYearBox() {
        year.removeItemListener(this)
        year.removeAllItems()

        with(openSession()) {
            selectList<Int>("get-years").forEach { year.addItem(it) }

            if (year.itemCount > 0) {
                year.selectedIndex = 0
            }

            close()
        }

        year.addItemListener(this)
    }

    @Suppress("HardCodedStringLiteral")
    private fun updateDayBox() {
        if (year.selectedItem == null || month.selectedItem == null) {
            day.removeAllItems()
            Logger.debug { "Nothing to do" }
        } else {
            day.removeItemListener(this)
            day.removeAllItems()
            day.addItem(Int.MIN_VALUE)

            with(openSession()) {
                val parameters = mapOf("year" to year.selectedItem, "month" to month.selectedItem)
                selectList<Int>("get-days-in-month", parameters).forEach { day.addItem(it) }
                close()
            }

            day.selectedIndex = 0
            day.addItemListener(this)
        }
    }

    @Suppress("HardCodedStringLiteral")
    private fun updateMonthBox() {
        if (year.selectedItem == null) {
            month.removeAllItems()
            Logger.debug { "No year selected" }
        } else {
            month.removeItemListener(this)
            month.removeAllItems()

            with(openSession()) {
                selectList<Int>("get-months-for-year", year.selectedItem).forEach {
                    month.addItem(it)
                }
                close()
            }

            month.selectedIndex = 0
            month.addItemListener(this)
        }
    }

    override fun itemStateChanged(e: ItemEvent) {
        if (e.stateChange == ItemEvent.SELECTED)
            if (e.source == month) {
                updateDayBox()
                reload()
            } else if (e.source == year) {
                updateMonthBox()
                updateDayBox()
                reload()
            } else if (e.source == day) {
                reload()
            }
    }

    private class TrashCellRenderer : DefaultTableCellRenderer() {
        val trashIcon: FontIcon = FontIcon.of(FontAwesomeRegular.TRASH_ALT, 18, Color.decode("#ff8531")) // NON-NLS
        val deletionIcon: FontIcon = FontIcon.of(FontAwesomeRegular.TIMES_CIRCLE, 18, Color.decode("#bc5090")) // NON-NLS

        override fun getTableCellRendererComponent(
            table: JTable,
            value: Any,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int,
        ): Component {
            val lbl = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column) as JLabel
            lbl.text = null
            lbl.horizontalAlignment = LEFT

            if (value is Number)
                if (value.toInt() == 1)
                    lbl.icon = trashIcon
                else
                    lbl.icon = deletionIcon
            return lbl
        }
    }

    private class TableModel : AbstractTableModel() {
        val formatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
        val columns = i18nString("history.columnNames").split(',').map { it.trim() }.toTypedArray()
        val data = mutableListOf<DeletionRecord>()

        fun clear() = data.clear()

        fun getRecord(row: Int): DeletionRecord = data[row]

        fun setData(records: Collection<DeletionRecord>) {
            data.clear()
            data.addAll(records)
            fireTableDataChanged()
        }

        override fun getRowCount(): Int = data.size

        override fun getColumnCount(): Int = columns.size

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
            return when (columnIndex) {
                0 -> formatter.format(Instant.ofEpochMilli(data[rowIndex].timestamp.toLong()).atZone(ZoneId.systemDefault()))
                1 -> Path(data[rowIndex].path).name
                2 -> data[rowIndex].extension.uppercase()
                3 -> data[rowIndex].size.getFormattedSize()
                4 -> data[rowIndex].trashed
                else -> ""
            }
        }

        override fun getColumnName(column: Int) = columns[column].uppercase()
    }

    @Suppress("HardCodedStringLiteral")
    private inner class HistoryLoader : SwingWorker<Void, Void>() {
        val records = ArrayList<DeletionRecord>()

        override fun doInBackground(): Void? {
            if (year.selectedItem == null || month.selectedItem == null || day.selectedItem == null)
                return null

            openSession().run {
                Logger.debug { "Loading history..." }
                val millis = measureTimeMillis {
                    if (day.selectedItem == Int.MIN_VALUE) {
                        val parameter = mapOf("year" to year.selectedItem, "month" to month.selectedItem)
                        selectList<DeletionRecord>("get-by-month", parameter).run { records.addAll(this) }
                    } else {
                        val parameter = mapOf("year" to year.selectedItem, "month" to month.selectedItem, "day" to day.selectedItem)
                        selectList<DeletionRecord>("get-by-day", parameter).run { records.addAll(this) }
                    }
                }
                Logger.debug { "History loaded in ${millis}ms" }
                close()
            }
            return null
        }

        override fun done() {
            myTableModel.setData(records)

            org.mth.execute {
                fileSizeTile.setText(records.sumOf { it.size }.getFormattedSize())
                fileCountTile.setText(records.size.toString())
                extensionTile.setText(records.distinctBy { it.extension }.count().toString())
            }
        }
    }
}