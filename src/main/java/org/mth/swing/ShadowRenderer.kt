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

import java.awt.Color
import java.awt.image.BufferedImage

class ShadowRenderer @JvmOverloads constructor(size: Int = 5, opacity: Float = 0.5f, color: Color = Color.BLACK) {
    var size: Int = 5
        private set

    var opacity: Float = 0.5f
        private set

    var color: Color = Color.BLACK
        private set

    init {
        this.size = size
        this.opacity = opacity
        this.color = color
    }

    fun createShadow(image: BufferedImage): BufferedImage {
        val shadowSize = size * 2
        val srcWidth = image.width
        val srcHeight = image.height
        val dstWidth = srcWidth + shadowSize
        val dstHeight = srcHeight + shadowSize
        val left = size
        val right = shadowSize - left
        val yStop = dstHeight - right
        val shadowRgb = color.rgb and 0x00FFFFFF
        val aHistory = IntArray(shadowSize)
        var historyIdx: Int
        var aSum: Int
        val dst = BufferedImage(
            dstWidth, dstHeight,
            BufferedImage.TYPE_INT_ARGB
        )
        val dstBuffer = IntArray(dstWidth * dstHeight)
        val srcBuffer = IntArray(srcWidth * srcHeight)
        GraphicsUtilities.getPixels(image, 0, 0, srcWidth, srcHeight, srcBuffer)
        val lastPixelOffset = right * dstWidth
        val hSumDivider = 1.0f / shadowSize
        val vSumDivider = opacity / shadowSize
        val hSumLookup = IntArray(256 * shadowSize)
        for (i in hSumLookup.indices) {
            hSumLookup[i] = (i * hSumDivider).toInt()
        }

        val vSumLookup = IntArray(256 * shadowSize)

        for (i in vSumLookup.indices) {
            vSumLookup[i] = (i * vSumDivider).toInt()
        }

        var srcOffset: Int
        var srcY = 0
        var dstOffset = left * dstWidth

        while (srcY < srcHeight) {
            historyIdx = 0
            while (historyIdx < shadowSize) {
                aHistory[historyIdx++] = 0
            }
            aSum = 0
            historyIdx = 0
            srcOffset = srcY * srcWidth
            for (srcX in 0..<srcWidth) {
                var a = hSumLookup[aSum]
                dstBuffer[dstOffset++] = a shl 24
                aSum -= aHistory[historyIdx]
                a = srcBuffer[srcOffset + srcX] ushr 24
                aHistory[historyIdx] = a
                aSum += a
                if (++historyIdx >= shadowSize) {
                    historyIdx -= shadowSize
                }
            }
            (0..<shadowSize).forEach { i ->
                val a = hSumLookup[aSum]
                dstBuffer[dstOffset++] = a shl 24
                aSum -= aHistory[historyIdx]
                if (++historyIdx >= shadowSize) {
                    historyIdx -= shadowSize
                }
            }
            srcY++
        }

        var x = 0
        var bufferOffset = 0

        while (x < dstWidth) {
            aSum = 0
            historyIdx = 0

            while (historyIdx < left) {
                aHistory[historyIdx++] = 0
            }

            run {
                var y = 0
                while (y < right) {
                    val a = dstBuffer[bufferOffset] ushr 24
                    aHistory[historyIdx++] = a
                    aSum += a
                    y++
                    bufferOffset += dstWidth
                }
            }

            bufferOffset = x
            historyIdx = 0

            run {
                var y = 0
                while (y < yStop) {
                    var a = vSumLookup[aSum]
                    dstBuffer[bufferOffset] = a shl 24 or shadowRgb
                    aSum -= aHistory[historyIdx]
                    a = dstBuffer[bufferOffset + lastPixelOffset] ushr 24
                    aHistory[historyIdx] = a
                    aSum += a
                    if (++historyIdx >= shadowSize) {
                        historyIdx -= shadowSize
                    }
                    y++
                    bufferOffset += dstWidth
                }
            }

            var y = yStop

            while (y < dstHeight) {
                val a = vSumLookup[aSum]
                dstBuffer[bufferOffset] = a shl 24 or shadowRgb
                aSum -= aHistory[historyIdx]
                if (++historyIdx >= shadowSize) {
                    historyIdx -= shadowSize
                }
                y++
                bufferOffset += dstWidth
            }

            x++
            bufferOffset = x
        }

        GraphicsUtilities.setPixels(dst, 0, 0, dstWidth, dstHeight, dstBuffer)

        return dst
    }
}
