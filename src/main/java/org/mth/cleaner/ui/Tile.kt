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

import com.formdev.flatlaf.ui.FlatUIUtils
import java.awt.*
import javax.swing.*
import javax.swing.border.EmptyBorder

class Tile(
    icon: ImageIcon,
    mainText: String,
    description: String,
    iconColor: Color = Color.orange,
    width: Int = 200,
) : JPanel() {

    private val mainLabel = JLabel().apply {
        text = mainText
        font = FlatUIUtils.nonUIResource(UIManager.getFont("large.font"))
    }

    /**
     * Bottom label, displaying text in smaller font
     */
    private val descriptionLabel = JLabel().apply {
        text = description
        font = FlatUIUtils.nonUIResource(UIManager.getFont("medium.font"))
    }

    fun setText(text: String) {
        mainLabel.text = text
    }

    init {
        background = Color.WHITE
        isOpaque = true
        preferredSize = Dimension(width, 72)
        minimumSize = preferredSize
        maximumSize = preferredSize
        border = EmptyBorder(15, 12, 15, 12)
        layout = BorderLayout()

        val iconLabel = object : JPanel() {
            val r = 40

            init {
                minimumSize = Dimension(46, 46)
                preferredSize = minimumSize
                isOpaque = false
            }

            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)

                val g2d = g as Graphics2D
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

                g2d.paint = iconColor
                g2d.fillRoundRect((minimumSize.width - r) / 2, (minimumSize.width - r) / 2, r, r, 9, 9)

                g2d.drawImage(icon.image, (minimumSize.width - icon.iconWidth) / 2, (minimumSize.width - icon.iconHeight) / 2, this)
            }
        }

        val labelPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            border = EmptyBorder(0, 10, 0, 0)

            add(Box.createVerticalGlue())
            add(mainLabel)
            add(descriptionLabel)
            add(Box.createVerticalGlue())

        }

        add(labelPanel, BorderLayout.CENTER)
        add(iconLabel, BorderLayout.WEST)
    }
}