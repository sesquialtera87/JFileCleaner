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
import net.miginfocom.swing.MigLayout
import org.mth.cleaner.ApplicationContext
import org.mth.cleaner.ApplicationContext.i18nString
import org.mth.cleaner.ApplicationContext.jobExecutor
import org.mth.execute
import org.mth.swing.LiquidProgress
import org.tinylog.Logger
import java.awt.Color
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.*
import javax.swing.border.LineBorder

class ProgressPopup : JDialog(ApplicationContext.FRAME, true) {

    private val messageLabel = JLabel().apply {
        horizontalAlignment = SwingConstants.LEFT
    }

    private var job: DeletionJob? = null

    private val progress = LiquidProgress().apply {
        isStringPainted = false
        value = 50
        borderColor = UIManager.getColor("primaryColor")
        animateColor = Color.decode("#F39C12") // NON-NLS
        borderSize = 4
    }

    init {
        isUndecorated = true

        layout = MigLayout("insets 10 0 10 5", "[60][grow, fill]") // NON-NLS
        preferredSize = Dimension(400, 120)
        rootPane.border = LineBorder(UIManager.getColor("lightPrimaryColor"), 2)

        val title = JLabel(i18nString("popup.title")).apply {
            font = FlatUIUtils.nonUIResource(UIManager.getFont("h1.font"))
        }

        add(title, "gapx 16, span, wrap") // NON-NLS
        add(progress, "gapy 10") // NON-NLS
        add(messageLabel, "gapx 10, growx") // NON-NLS

        pack()
        addWindowListener(DialogListener())
    }

    fun message(text: String) {
        execute { messageLabel.text = text }
    }

    fun addJob(job: DeletionJob) {
        this.job = job
        job.addPropertyChangeListener {
            if (it.propertyName == "state")
                if (it.newValue == SwingWorker.StateValue.DONE) {
                    SwingUtilities.invokeLater { dispose() }
                    this.job = null
                }
        }
    }

    inner class DialogListener : WindowAdapter() {
        override fun windowOpened(e: WindowEvent) {
            Logger.debug { "Dialog opened" } // NON-NLS

            if (job != null)
                jobExecutor.execute(job!!)
            else
                Logger.debug { "Null job" } // NON-NLS
        }

        override fun windowClosing(e: WindowEvent?) {
            Logger.debug { "Dialog closing" } // NON-NLS
        }
    }
}