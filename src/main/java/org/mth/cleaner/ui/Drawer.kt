package org.mth.cleaner.ui

import net.miginfocom.swing.MigLayout
import org.mth.cleaner.ApplicationContext.getSvgIcon
import org.mth.cleaner.ApplicationContext.i18nString
import org.mth.execute
import java.awt.BorderLayout
import java.awt.Dimension
import java.util.*
import javax.swing.*
import javax.swing.border.EmptyBorder

class Drawer(val scenePanel: JPanel) : JPanel() {

    abstract class Scene : JPanel() {
        abstract fun onShow()

        open fun onHide() {}

        open fun getPreferences(): Properties = Properties()

        open fun injectPreferences(properties: Properties) {}
    }

    private data class SceneInfo(val title: String, val icon: Icon, val scene: Scene)

    private val scenes = ArrayList<SceneInfo>(3)

    private val buttonPanel = JPanel(MigLayout("insets 0 0 0 0", "[fill, grow]")).apply { // NON-NLS
        isOpaque = false
    }

    fun getScenes(): Collection<Scene> = scenes.map { it.scene }

    fun addScene(title: String, icon: Icon, scene: Scene) {
        scene.isOpaque = false
        scene.background = UIManager.getColor("sceneBackground")
        scenes.add(SceneInfo(title, icon, scene))

        if (scenes.size == 1)
            buttonPanel.add(createSceneButton(SceneInfo(title, icon, scene)), "wrap, growx, gapy 25") // NON-NLS
        else
            buttonPanel.add(createSceneButton(SceneInfo(title, icon, scene)), "wrap, growx") // NON-NLS

        if (scenes.size == 1)
            applyScene(scenes[0])
    }

    private fun applyScene(info: SceneInfo) = execute {
        scenePanel.removeAll()
        scenePanel.add(info.scene, BorderLayout.CENTER)

        buttonPanel.components.filterIsInstance<JButton>().forEach {
            val sceneInfo = it.getClientProperty("info") as SceneInfo

            if (info == sceneInfo) {
                it.background = UIManager.getColor("lightPrimaryColor")
            } else {
                it.background = UIManager.getColor("white")
            }
        }

        SwingUtilities.updateComponentTreeUI(info.scene)
        info.scene.onShow()
    }

    private fun createSceneButton(info: SceneInfo) =
        JButton(info.title).apply {
            border = EmptyBorder(0, 0, 0, 15)
            foreground = UIManager.getColor("primaryForeground")
            background = UIManager.getColor("white")
            font = UIManager.getFont("large.font")
            minimumSize = Dimension(100, 50)
            iconTextGap = 9
            icon = info.icon
            putClientProperty("info", info)
            addActionListener {
                if (info.scene !in scenePanel.components) {
                    applyScene(info)
                }
            }
        }

    init {
        layout = BorderLayout()
        isOpaque = true
        background = UIManager.getColor("white")
        minimumSize = Dimension(160, 200)
        preferredSize = minimumSize

        val appIconLabel = JLabel().apply {
            icon = getSvgIcon("clear.svg", 46, 46)
            horizontalAlignment = SwingConstants.CENTER
        }

        val appNameLabel = JLabel().apply {
            text = i18nString("app.shortName")
            horizontalAlignment = SwingConstants.CENTER
            font = UIManager.getFont("h1.regular.font")
        }

        val versionLabel = JLabel().apply {
            text = "<html>%s<br/>version %s".format(i18nString("app.name"), i18nString("app.version")) // NON-NLS
            font = UIManager.getFont("medium.font")
            horizontalAlignment = SwingConstants.CENTER
            border = EmptyBorder(0, 0, 15, 0)
        }

        buttonPanel.add(appIconLabel, "gapy 20, wrap") // NON-NLS
        buttonPanel.add(appNameLabel, "wrap") // NON-NLS

        add(buttonPanel, BorderLayout.CENTER)
        add(versionLabel, BorderLayout.SOUTH)
    }
}
