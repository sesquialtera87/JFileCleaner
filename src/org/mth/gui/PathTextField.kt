package org.mth.gui

import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.KeyboardFocusManager
import java.awt.Window
import java.awt.event.*
import java.io.File
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class PathTextField : JTextField() {

    private val popup = Window(null)
    private val completionListModel = DefaultListModel<String>()
    private val completionList = JList(completionListModel)
    private val pathSeparators = setOf('\\', '/')
    private val documentListener = object : DocumentListener {
        override fun insertUpdate(e: DocumentEvent?) {
            println("insert")

            if (popup.isVisible) {
                println("UPDATE")
                val separatorPosition = getSeparatorIndex()
                val partialInput = text.substring(separatorPosition + 1, caretPosition + 1).lowercase()
                println("hint = $partialInput")

                for (i in completionListModel.size() - 1 downTo 0) {
                    if (!completionListModel[i].lowercase().startsWith(partialInput)) {
                        completionListModel.remove(i)
                    }
                }

                completionList.selectedIndex = 0
            }
        }

        override fun removeUpdate(e: DocumentEvent?) {
            println("remove")
        }

        override fun changedUpdate(e: DocumentEvent?) {
            println("change")
        }
    }

    init {
        actionMap.put("delete-previous-word", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                // if the caret is next to a separator char, then we need to search for another separator antecedent to this
                if (caretPosition > 0)
                    if (document.getText(caretPosition - 1, 1)[0] in pathSeparators) {
                        // remove the first separator sign
                        document.remove(caretPosition - 1, 1)
                    }

                val separatorIndex = getSeparatorIndex()
                document.remove(separatorIndex + 1, document.length - separatorIndex - 1)
            }
        })

        setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, emptySet())

        with(popup) {
            layout = BorderLayout()
            minimumSize = Dimension(150, 200)
            add(JScrollPane(completionList), BorderLayout.CENTER)
        }

        completionList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2 && e.button == MouseEvent.BUTTON1) {
                    injectCompletion(getSeparatorIndex(), completionList.selectedValue)
                    popup.isVisible = false
                }
            }
        })

        document.addDocumentListener(documentListener)

        addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {

                when (e.keyCode) {
                    KeyEvent.VK_SPACE -> {
                        if (e.isControlDown) {
                            println("completion request")
                            completeAtCaret()
                        }
                    }
                    KeyEvent.VK_TAB -> {
                        println("completion request")
                        completeAtCaret()
                    }
                    KeyEvent.VK_DOWN -> {
                        if (popup.isVisible)
                            with(completionList) {
                                if (selectedIndex < completionListModel.size() - 1) {
                                    selectedIndex++
                                    scrollRectToVisible(getCellBounds(selectedIndex, selectedIndex))
                                }
                            }
                    }
                    KeyEvent.VK_UP -> {
                        if (popup.isVisible)
                            with(completionList) {
                                if (selectedIndex > 0) {
                                    selectedIndex--
                                    scrollRectToVisible(getCellBounds(selectedIndex, selectedIndex))
                                }
                            }
                    }
                    KeyEvent.VK_ENTER -> {
                        if (popup.isVisible) {
                            injectCompletion(getSeparatorIndex(), completionList.selectedValue)
                            popup.isVisible = false
                        }
                    }
                    KeyEvent.VK_ESCAPE -> popup.isVisible = false
                }
            }

            override fun keyReleased(e: KeyEvent?) {

            }
        })
    }

    private fun buildPopup(folders: Collection<String>) {
        folders.forEach { println(it) }
        completionListModel.clear()
        completionListModel.addAll(folders)
    }

    private fun showPopup() {
        if (completionListModel.isEmpty)
            return

        val stringWidth = graphics.fontMetrics.stringWidth(text.substring(0, caretPosition))
        popup.setLocation(locationOnScreen.x + stringWidth, locationOnScreen.y + height + 1)
        popup.isVisible = true
        popup.requestFocus()

        completionList.selectedIndex = 0
    }

    private fun injectCompletion(separatorPosition: Int, completion: String) {
        document.remove(separatorPosition + 1, document.length - separatorPosition - 1)
        document.insertString(separatorPosition + 1, completion + "\\", null)
    }

    private fun getSeparatorIndex(): Int {
        var separatorIndex = -1

        for (i in IntRange(0, caretPosition - 1).reversed()) {
            if (text[i] in pathSeparators) {
                separatorIndex = i
                break
            }
        }

        return separatorIndex
    }

    fun completeAtCaret() {
        val separatorIndex = getSeparatorIndex()

        if (separatorIndex < 0)
            return

        var parent = text.substring(0, separatorIndex)
        val partialInput = text.substring(separatorIndex + 1, caretPosition).lowercase()

        if (parent.matches("[A-Z]:".toRegex())) {
            parent += "\\"
        }

        val parentPath = File(parent)

        println(parent)
        println(partialInput)

        if (!parentPath.exists()) {
            System.err.println("Invalid directory")
            return
        }

        val completions = getCompletions(partialInput, parentPath)

        if (completions.size == 1) {
            injectCompletion(separatorIndex, completions.first())
        } else {
            buildPopup(completions)
            showPopup()
        }
    }

    private fun getCompletions(partialInput: String, parentPath: File): List<String> {
        val files = if (partialInput.isEmpty())
            parentPath.listFiles { file -> file.isDirectory }
        else
            parentPath.listFiles { file ->
                file.isDirectory && file.name.lowercase().startsWith(partialInput)
            }

        return files?.map { it.name } ?: emptyList()
    }
}

fun main() {
    JFrame().apply {
        layout = BorderLayout()
        preferredSize = Dimension(500, 300)
        add(PathTextField().apply { text = "C:\\" }, BorderLayout.NORTH)
        pack()
        defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        isVisible = true
    }
}