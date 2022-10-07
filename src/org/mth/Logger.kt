package org.mth

import org.kordamp.ikonli.fontawesome.FontAwesome
import org.kordamp.ikonli.swing.FontIcon
import java.awt.Color
import javax.swing.Icon
import javax.swing.ImageIcon
import javax.swing.JTextPane
import javax.swing.SwingUtilities
import javax.swing.text.*
import javax.swing.text.StyleConstants as Styler


/**
 *
 * @author mattia marelli
 */
object Logger {

    private lateinit var textComponent: JTextPane
    private lateinit var doc: StyledDocument
    private var sc: StyleContext = StyleContext.getDefaultStyleContext()


    lateinit var headingStyle: Style
    lateinit var greenStyle: Style
    lateinit var defaultStyle: Style
    lateinit var boldBlue: Style
    lateinit var darkOrange: Style
    lateinit var warningStyle: Style
    lateinit var timestampStyle: Style
    lateinit var assignmentStyle: Style
    lateinit var defaultUnderlineStyle: Style

    lateinit var skipIcon: Icon

    fun initialize(textPane: JTextPane) {
        textComponent = textPane
        doc = textComponent.styledDocument

        textComponent.editorKit = StyledEditorKit()
        textComponent.document = doc
        textComponent.caretColor = textComponent.background
        // inibisce l'utilizzo dell'area di testo
        textComponent.isFocusable = false

        defaultStyle = sc.getStyle(StyleContext.DEFAULT_STYLE)
        Styler.setTabSet(defaultStyle, getTabSet(2)) // todo

        defaultUnderlineStyle = sc.addStyle("defaultUnderline", defaultStyle)
        Styler.setUnderline(defaultUnderlineStyle, true)

        headingStyle = sc.addStyle("heading", defaultStyle)
        Styler.setForeground(headingStyle, Color.LIGHT_GRAY)

        greenStyle = sc.addStyle("green", defaultStyle)
        Styler.setForeground(greenStyle, Color(98, 151, 85))

        darkOrange = sc.addStyle("darkOrange", defaultStyle)
        Styler.setForeground(darkOrange, Color(138, 101, 59))

        warningStyle = sc.addStyle("warning", defaultStyle)
        Styler.setForeground(warningStyle, Color(255, 198, 109))

        timestampStyle = sc.addStyle("timestamp", defaultStyle)
        Styler.setForeground(timestampStyle, Color(152, 118, 187))

        assignmentStyle = sc.addStyle("assignment", defaultStyle)
        Styler.setForeground(assignmentStyle, Color(104, 150, 187))

        boldBlue = sc.addStyle("boldBlue", defaultStyle)
        Styler.setForeground(boldBlue, Color(70, 124, 218))
        Styler.setBold(boldBlue, true)

        skipIcon = ImageIcon("/icon/skip-16.png")
    }

    fun icon(icon: Icon): Logger {
        textComponent.caretPosition = doc.length
        textComponent.insertIcon(icon)
        append("  ")

        return this
    }

    fun logTime() {
        icon(FontIcon.of(FontAwesome.SLACK, 10, Color.LIGHT_GRAY))
        append(formattedTimeStamp(), timestampStyle)
            .append("\n")
    }

    private fun getTabSet(charactersPerTab: Int): TabSet {
        val fm = textComponent.getFontMetrics(textComponent.font)
        val charWidth = fm.charWidth('w')
        val tabWidth = charWidth * charactersPerTab

        val tabs = arrayOfNulls<TabStop>(10)

        for (j in tabs.indices) {
            val tab = j + 1
            tabs[j] = TabStop((tab * tabWidth).toFloat())
        }

        return TabSet(tabs)
    }

    fun append(msg: String, style: MutableAttributeSet = defaultStyle): Logger {
        execute {
            doc.insertString(doc.length, msg, style)

            /* move the cursor at the end */
            textComponent.caretPosition = doc.length
        }

        return this
    }

    fun message(msg: String, style: Style = defaultStyle) {
        append(msg + "\n", style)
    }

    fun blankLine() {
        message("\n")
    }

    fun clear() {
        textComponent.text = ""
    }
}