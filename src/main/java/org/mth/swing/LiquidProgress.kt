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

import java.awt.*
import java.awt.geom.Area
import java.awt.geom.CubicCurve2D
import java.awt.geom.GeneralPath
import javax.swing.JProgressBar

class LiquidProgress : JProgressBar() {

    private val UI: LiquidProgressUI = LiquidProgressUI(this)

    @JvmField
    var borderSize: Int = 5

    @JvmField
    var spaceSize: Int = 5

    @JvmField
    var animateColor: Color = Color(125, 216, 255)

    @JvmField
    var borderColor: Color = Color(0, 178, 255)

    init {
        font = Font(getFont().family, 1, 20)
        isOpaque = false
        preferredSize = Dimension(100, 100)
        background = Color.WHITE
        foreground = Color(0, 178, 255)
        isStringPainted = true
        setUI(UI)
    }

    fun startAnimation() = UI.start()

    fun stopAnimation() = UI.stop()

    companion object {
        fun createWaterStyle(size: Rectangle): Shape {
            val width = size.getWidth()
            val height = size.getHeight()
            val space = width / 4
            val x = size.getX()
            val y = size.getY()
            val gp1 = GeneralPath(CubicCurve2D.Double(x, y + height, x + space, y + height, x + space, y, x + width / 2, y))
            gp1.lineTo(x + width / 2, y + height)
            val gp2 = GeneralPath(CubicCurve2D.Double(x + width / 2, y, x + width - space, y, x + width - space, y + height, x + width, y + height))
            gp2.lineTo(x + width / 2, y + height)
            val area = Area(gp1)
            area.add(Area(gp2))
            return area
        }
    }
}