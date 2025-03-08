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

package org.mth.swing

import org.mth.swing.LiquidProgress.Companion.createWaterStyle
import org.tinylog.Logger
import java.awt.*
import java.awt.geom.Ellipse2D
import java.awt.image.BufferedImage
import javax.swing.JComponent
import javax.swing.plaf.basic.BasicProgressBarUI
import kotlin.math.min

class LiquidProgressUI(private val liquidProgress: LiquidProgress) : BasicProgressBarUI() {
    private var thread: Thread? = null
    private var start = false
    private var location1 = -1f
    private var location2 = 0f

    init {
        start()
    }

    fun start() {
        if (!start) {
            start = true
            thread = Thread(Runnable {
                while (start) {
                    location1 += 0.01f
                    location2 += 0.01f

                    if (location1 > 1f) {
                        location1 = -1f
                    }

                    if (location2 > 1f) {
                        location2 = -1f
                    }

                    liquidProgress.repaint()
                    sleep()
                }
            })
            thread!!.start()
        }
    }

    fun stop() {
        start = false
    }

    private fun sleep() {
        try {
            Thread.sleep(5)
        } catch (e: InterruptedException) {
            Logger.trace(e)
        }
    }

    override fun paint(g: Graphics?, component: JComponent) {
        val g2 = g as Graphics2D
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val width = component.getWidth()
        val height = component.getHeight()
        var size = min(width.toDouble(), height.toDouble()).toInt()
        val x = (width - size) / 2
        val y = (height - size) / 2

        if (liquidProgress.getParent() != null) {
            val color = liquidProgress.getParent().getBackground()
            g2.color = color
            g2.fillRect(0, 0, width, height)
        }

        g2.color = liquidProgress.borderColor
        g2.fillOval(x, y, size, size)
        var borderSize = liquidProgress.borderSize
        size -= borderSize * 2
        g2.color = liquidProgress.getBackground()
        g2.fillOval(x + borderSize, y + borderSize, size, size)
        val spaceSize = liquidProgress.spaceSize
        borderSize += spaceSize
        size -= spaceSize * 2
        createAnimation(g2, x + borderSize, y + borderSize, size)

        if (progressBar.isStringPainted) {
            paintString(g)
        }

        g2.dispose()
    }

    private fun createAnimation(graphics: Graphics2D, x: Int, y: Int, size: Int) {
        val img = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g2 = img.createGraphics()
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val circle: Ellipse2D = Ellipse2D.Double(0.0, 0.0, size.toDouble(), size.toDouble())
        g2.color = progressBar.getBackground()
        g2.fill(circle)
        g2.composite = AlphaComposite.SrcIn
        val value = (size * progressBar.getPercentComplete()).toInt()
        val waterStyleHeight = (size * 0.07f).toInt() //  Height 7% of Size
        g2.color = liquidProgress.animateColor
        g2.fillRect(0, size - value, size, size)
        g2.fill(createWaterStyle(Rectangle((location1 * size).toInt(), size - value - waterStyleHeight, size, waterStyleHeight)))
        g2.fill(createWaterStyle(Rectangle((location2 * size).toInt(), size - value - waterStyleHeight, size, waterStyleHeight)))
        g2.dispose()
        graphics.drawImage(img, x, y, null)
    }

    private fun paintString(g: Graphics) {
        val b = progressBar.getInsets()
        val barRectWidth = progressBar.getWidth() - b.right - b.left
        val barRectHeight = progressBar.getHeight() - b.top - b.bottom
        g.color = progressBar.getForeground()
        paintString(g, b.left, b.top, barRectWidth, barRectHeight, 0, b)
    }
}
