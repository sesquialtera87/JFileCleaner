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

import org.kordamp.ikonli.fontawesome5.FontAwesomeRegular
import org.kordamp.ikonli.swing.FontIcon
import org.mth.cleaner.ApplicationContext
import org.mth.execute
import org.tinylog.Logger
import java.awt.*
import java.awt.event.*
import java.io.File
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.util.function.Consumer
import javax.swing.*
import javax.swing.KeyStroke.getKeyStroke
import javax.swing.border.EmptyBorder
import javax.swing.border.LineBorder
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.math.min

class ChronologyPopup(owner: JFrame, val textField: JTextField) : JWindow(owner), FocusListener {

    var onItemClick: Consumer<Path> = Consumer { Logger.warn { "No action assigned!" } } // NON-NLS

    private val folderIcon = FontIcon.of(FontAwesomeRegular.FOLDER, 19, Color.ORANGE)
    private val selectedFolderIcon = FontIcon.of(FontAwesomeRegular.FOLDER, 19, Color.ORANGE)
    private val svgIcon: Icon = ApplicationContext.getSvgIcon("history.svg", 20, 20, Color.white, UIManager.getColor("primaryForeground"))
    private val selectedSvgIcon: Icon = ApplicationContext.getSvgIcon("history.svg", 20, 20, Color.white, UIManager.getColor("white"))
    private val contentPanel = JPanel()
    private val completions = mutableListOf<String>()
    private var popupCells = Array<Label>(10) { Label("") }
    private var enteredText: String = ""
    private var completionOffset: Int = -1
    private var chronologyOnly = false

    private val hidePopupAction = makeAction {
        if (isVisible) {
            Logger.debug { "Closing popup" } // NON-NLS
            dispose()
        }
    }

    private val completeAction = makeAction {
        chronologyOnly = false
        completeAtCaret()
    }

    private val showCompleteChronology = makeAction {
        chronologyOnly = true
        completions.clear()
        updatePopupContent()
        showPopup()
    }

    private val acceptFirst = makeAction {
        if (isVisible) {
            contentPanel.components.firstOrNull().let {
                val l = it as Label
                l.acceptItem()
            }
        }
    }

    private val deletePreviousDir = makeAction {
        Logger.debug { "Delete previous dir" } // NON-NLS

        textField.run {
            // if the caret is next to a separator char, then we need to search for another separator antecedent to this
            if (caretPosition > 0)
                if (document.getText(caretPosition - 1, 1)[0] == File.separatorChar) {
                    // remove the first separator sign
                    document.remove(caretPosition - 1, 1)
                }

            val separatorIndex = getSeparatorIndex()
            document.remove(separatorIndex + 1, document.length - separatorIndex - 1)
        }
    }

    private val documentListener = object : DocumentListener {
        override fun insertUpdate(e: DocumentEvent) {
            Logger.debug { "insert" } //NON-NLS

            if (textField.hasFocus())
                completeAtCaret()
        }

        override fun removeUpdate(e: DocumentEvent) {
            if (textField.hasFocus())
                completeAtCaret()
        }

        override fun changedUpdate(e: DocumentEvent) {
            Logger.debug { "change" } //NON-NLS
        }
    }

    init {
        textField.addFocusListener(this)
        textField.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, emptySet())
        textField.document.addDocumentListener(documentListener)

        installTextFieldActions()

        contentPanel.border = LineBorder(UIManager.getColor("primaryColor"), 1)
        add(contentPanel, BorderLayout.CENTER)
    }

    private fun getSeparatorIndex(): Int {
        var separatorIndex = -1
        val text = textField.text

        for (i in IntRange(0, min(textField.caretPosition, text.length) - 1).reversed()) {
            if (text[i] == File.separatorChar) {
                separatorIndex = i
                break
            }
        }

        return separatorIndex
    }

    private fun installTextFieldActions() {
        textField.actionMap.run {
            put(hidePopupAction, hidePopupAction)
            put(deletePreviousDir, deletePreviousDir)
            put(completeAction, completeAction)
            put(acceptFirst, acceptFirst)
            put(showCompleteChronology, showCompleteChronology)
        }

        textField.inputMap.run {
            put(getKeyStroke(KeyEvent.VK_SPACE, KeyEvent.CTRL_DOWN_MASK), completeAction)
            put(getKeyStroke("control shift SPACE"), showCompleteChronology)
            put(getKeyStroke("TAB"), completeAction)
            put(getKeyStroke("ESCAPE"), hidePopupAction)
            put(getKeyStroke(KeyEvent.VK_BACK_SPACE, KeyEvent.SHIFT_DOWN_MASK), deletePreviousDir)
            put(getKeyStroke("shift ENTER"), acceptFirst)
        }
    }

    fun showPopup() {
        size = Dimension(textField.width, 30 * contentPanel.components.size)

        val loc = textField.locationOnScreen
        loc.translate(0, textField.height + 1)
        location = loc
        isVisible = true
    }

    private fun expandAvailableCells(size: Int) {
        if (size <= popupCells.size) return

        val cells = Array<Label>(size) {
            if (it < popupCells.size)
                popupCells[it]
            else
                Label()
        }

        popupCells = cells
    }

    override fun focusGained(e: FocusEvent) {
//        showPopup()
    }

    override fun focusLost(e: FocusEvent) {
        dispose()
    }

    @Suppress("HardCodedStringLiteral")
    fun completeAtCaret() {
        textField.run {
            val separatorIndex = getSeparatorIndex()

            if (separatorIndex < 0)
                return

            var parent = text.substring(0, separatorIndex)
            val input = text.substring(separatorIndex + 1, text.length).lowercase().trim()

            // no new search needed
            if (input.isNotEmpty() && input == enteredText)
                return

            enteredText = input

            completionOffset = separatorIndex + 1

            // Windows root
            if (parent.matches("[A-Z]:".toRegex())) {
                parent += "\\"
            }

            val parentPath = File(parent)

            Logger.debug { "parent=$parentPath, hint=$enteredText" }

            if (!parentPath.exists()) {
                Logger.warn { "Invalid directory" }
                return
            }

            getCompletions(enteredText, parentPath)

            updatePopupContent()

            showPopup()
        }
    }

    private fun getCompletions(partialInput: String, parentPath: File): List<String> {
        completions.clear()

        val files = if (partialInput.isEmpty())
            parentPath.listFiles { file -> file.isDirectory }
        else
            parentPath.listFiles { file ->
                file.isDirectory && file.name.lowercase().startsWith(partialInput)
            }

        if (files != null) {
            completions.addAll(files.map { it.name })
        }

        return completions
    }

    private fun updatePopupContent() {
        val currentPath = try {
            Path.of(textField.text)
        } catch (_: InvalidPathException) {
            return
        }

        val chronology = if (chronologyOnly) ApplicationContext.getChronology(Int.MAX_VALUE)
            .filter { it.path != currentPath }
        else ApplicationContext.getChronology(Int.MAX_VALUE)
            .filter { it.path != currentPath }
            .take(3)

        if (chronology.isEmpty() && completions.isEmpty()) {
            Logger.debug { "Empty chronology and no completions. No need to show the popup." } // NON-NLS
            return
        }

        expandAvailableCells(chronology.size + completions.size)

        contentPanel.removeAll()
        contentPanel.layout = GridLayout(chronology.size + completions.take(10).size, 1)

        execute {
            completions
                .take(10)
                .map { Pair(it, false) }
                .plus(chronology.map { Pair(it.path.absolutePathString(), true) })
                .mapIndexed { i, p ->
                    popupCells[i].apply {
                        content = p.first
                        recent = p.second
                        refresh()
                    }
                }.forEach { contentPanel.add(it, true) }
            SwingUtilities.updateComponentTreeUI(contentPanel)
        }
    }

    private fun makeAction(actionListener: ActionListener) = object : AbstractAction() {
        override fun actionPerformed(e: ActionEvent) {
            actionListener.actionPerformed(e)
        }
    }

    inner class Label(var content: String = "", var recent: Boolean = true) : JLabel(), MouseMotionListener, MouseListener {
        override fun mouseDragged(e: MouseEvent) {
        }

        override fun mouseMoved(e: MouseEvent) {
        }

        override fun mouseClicked(e: MouseEvent) {
            if (SwingUtilities.isLeftMouseButton(e))
                if (e.clickCount == 1) {
                    acceptItem()
                }
        }

        fun acceptItem() {
            if (recent)
                onItemClick.accept(Path(content))
            else {
                textField.document.removeDocumentListener(documentListener)
                textField.text = textField.text.removeRange(completionOffset, textField.text.length) + content + File.separator
                textField.document.addDocumentListener(documentListener)
                completeAtCaret()
            }
        }

        override fun mousePressed(e: MouseEvent) {
        }

        override fun mouseReleased(e: MouseEvent) {
        }

        override fun mouseEntered(e: MouseEvent) {
            background = UIManager.getColor("primaryColor")
            foreground = UIManager.getColor("white")
            icon = if (recent) selectedSvgIcon else selectedFolderIcon
        }

        override fun mouseExited(e: MouseEvent) {
            background = UIManager.getColor("white")
            foreground = UIManager.getColor("Label.foreground")
            icon = if (recent) svgIcon else folderIcon
        }

        fun refresh() {
            text = content
            icon = if (recent) svgIcon else folderIcon
            background = UIManager.getColor("white")
            isOpaque = true
        }

        init {
            text = content
            icon = svgIcon
            iconTextGap = 10
            preferredSize = Dimension(50, 30)
            minimumSize = preferredSize
            border = EmptyBorder(0, 5, 0, 0)
            background = UIManager.getColor("white")
            isOpaque = true
            addMouseListener(this)
            addMouseMotionListener(this)
        }
    }
}

fun main() {
    ApplicationContext.addToChronology(Path("C:\\Users\\matti\\OneDrive\\Documenti\\Java\\JFileCleaner\\src\\main\\java\\org\\mth\\gui"))
    ApplicationContext.addToChronology(Path("C:\\Users\\matti\\OneDrive\\Documenti\\Java\\JFileCleaner\\src\\main\\resources"))
    ApplicationContext.addToChronology(Path("C:\\Users\\matti\\OneDrive\\Documenti\\Java\\JFileCleaner\\src\\main"))
    ApplicationContext.addToChronology(Path("C:\\Users\\matti\\OneDrive\\Documenti\\Java\\JFileCleaner\\src"))

    val field = JTextField("C:\\")

    JFrame().apply {
        val popup = ChronologyPopup(this, field)
        layout = BorderLayout()
        preferredSize = Dimension(500, 300)
        add(field, BorderLayout.NORTH)
        pack()
        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        isVisible = true
    }
}