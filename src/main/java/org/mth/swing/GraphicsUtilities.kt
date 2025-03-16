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

import java.awt.image.BufferedImage
import java.awt.image.Raster

object GraphicsUtilities {
    fun getPixels(
        img: BufferedImage,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        pixels: IntArray?,
    ): IntArray? {
        var pixels = pixels

        if (w == 0 || h == 0)
            return IntArray(0)

        if (pixels == null) {
            pixels = IntArray(w * h)
        } else require(pixels.size >= w * h) { "pixels array must have a length >= w*h" } // NON-NLS

        val imageType = img.type

        if (imageType == BufferedImage.TYPE_INT_ARGB || imageType == BufferedImage.TYPE_INT_RGB) {
            val raster: Raster = img.raster
            return raster.getDataElements(x, y, w, h, pixels) as IntArray?
        }

        // Unmanaged the image
        return img.getRGB(x, y, w, h, pixels, 0, w)
    }

    fun setPixels(
        img: BufferedImage,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        pixels: IntArray?,
    ) {
        if (pixels == null || w == 0 || h == 0) return
        else require(pixels.size >= w * h) { "pixels array must have a length >= w*h" } // NON-NLS

        val imageType = img.type

        if (imageType == BufferedImage.TYPE_INT_ARGB || imageType == BufferedImage.TYPE_INT_RGB) {
            val raster = img.raster
            raster.setDataElements(x, y, w, h, pixels)
        } else {
            // Un-manages the image
            img.setRGB(x, y, w, h, pixels, 0, w)
        }
    }
}
