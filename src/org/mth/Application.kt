package org.mth

import org.kordamp.ikonli.fontawesome.FontAwesome
import org.kordamp.ikonli.fontawesome5.FontAwesomeRegular
import org.kordamp.ikonli.swing.FontIcon
import org.mth.gui.FieldTransferHandler
import org.mth.gui.JExtensionList
import org.mth.gui.PathTextField
import org.mth.sqlite.FileExtension
import org.mth.sqlite.Folder
import org.mth.sqlite.openSession
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.util.function.Consumer
import javax.swing.*
import javax.swing.KeyStroke.getKeyStroke
import javax.swing.border.EmptyBorder

object Application : JFrame() {

    private var currentDirectory: File = File(System.getProperty("user.dir"), "file_test")
    private val directoryField = PathTextField()
    private val extensionList = JExtensionList()
    private val fileChooser = JFileChooser()
    private val trashCheckBox = JCheckBox()
    private val logArea = JTextPane()

    init {
        Logger.initialize(logArea)

        title = "JFileCleaner"
        contentPane = JPanel(BorderLayout())
        iconImage = FontIcon.of(FontAwesome.BITBUCKET, 32, Color.LIGHT_GRAY).toImageIcon().image

        initActions()

        extensionList.componentPopupMenu = createExtensionListPopup()

        with(directoryField) {
            action = SelectDirectoryAction
            // add drop capability to the TextField

            // add drop capability to the TextField
            transferHandler = FieldTransferHandler().apply {
                dropFunction = { file -> update(file) }
            }
        }

        // init FileChooser
        with(fileChooser) {
            currentDirectory = currentDirectory
            isMultiSelectionEnabled = false
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        }

        with(trashCheckBox) {
            text = "Use system trash"
            isSelected = Desktop.getDesktop().isSupported(Desktop.Action.MOVE_TO_TRASH)
            isEnabled = Desktop.getDesktop().isSupported(Desktop.Action.MOVE_TO_TRASH)
            alignmentX = JComponent.LEFT_ALIGNMENT
        }

        val browseButton = JButton().apply {
            action = BrowseAction
            text = "Browse"
            mnemonic = KeyEvent.VK_B
            icon = FontIcon.of(FontAwesomeRegular.FOLDER_OPEN, 18, Color.LIGHT_GRAY)
        }

        layout = BorderLayout()

        val topPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.LINE_AXIS)
            border = EmptyBorder(2, 0, 14, 0)

            add(directoryField)
            add(browseButton)
        }

        val extensionLabel = JLabel().apply {
            text = "Available extensions"
            alignmentX = Component.LEFT_ALIGNMENT
            icon = FontIcon.of(FontAwesomeRegular.LIST_ALT, 18, Color.LIGHT_GRAY)
        }

        val listScroll = JScrollPane(extensionList)
        listScroll.alignmentX = Component.LEFT_ALIGNMENT
        listScroll.preferredSize = Dimension(200, 20000)

        val extensionPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.PAGE_AXIS)

            add(extensionLabel)
            add(Box.createRigidArea(Dimension(0, 4)))
            add(listScroll)
            add(Box.createRigidArea(Dimension(0, 4)))
            add(trashCheckBox)
        }

        val logLabel = JLabel().apply {
            text = "Log"
            icon = FontIcon.of(FontAwesomeRegular.COMMENT_DOTS, 18, Color.LIGHT_GRAY)
            alignmentX = JComponent.LEFT_ALIGNMENT
        }

        val logPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.PAGE_AXIS)

            add(logLabel)
            add(Box.createRigidArea(Dimension(0, 4)))
            add(JScrollPane(logArea).apply { alignmentX = JComponent.LEFT_ALIGNMENT })
        }

        val cleanButton = JButton().apply {
            action = DeleteAction
            text = "Clean"
            mnemonic = KeyEvent.VK_C
            preferredSize = Dimension(160, 27)
        }

        val bottomPanel = JPanel().apply {
            layout = FlowLayout()
            border = EmptyBorder(6, 0, 0, 0)
            add(cleanButton)
        }

        val centerPanel = JPanel().apply {
            layout = GridLayout(1, 2, 6, 0)
            add(extensionPanel)
            add(logPanel)
        }

        with(contentPane as JPanel) {
            border = EmptyBorder(8, 8, 8, 8)

            add(topPanel, BorderLayout.NORTH)
            add(centerPanel, BorderLayout.CENTER)
            add(bottomPanel, BorderLayout.SOUTH)
        }

        defaultCloseOperation = EXIT_ON_CLOSE
        preferredSize = Dimension(500, 400)
        pack()
        setLocationRelativeTo(null)

        update(currentDirectory)
    }

    private fun createExtensionListPopup(): JPopupMenu {
        val popup = JPopupMenu()

        popup.add(JMenuItem().apply {
            text = "Reload"
            icon = FontIcon.of(FontAwesome.REFRESH, 14, Color.LIGHT_GRAY)
            addActionListener { findExtensions(currentDirectory) }
        })
        popup.add(JMenuItem().apply {
            text = "Select all"
            addActionListener { extensionList.checkAll(true) }
        })
        popup.add(JMenuItem().apply {
            text = "Deselect all"
            addActionListener { extensionList.checkAll(false) }
        })

        popup.addSeparator()

        popup.add(JMenuItem().apply {
            action = SaveAction
            icon = FontIcon.of(FontAwesome.SAVE, 14, Color.LIGHT_GRAY)
            text = "Save preferences"
        })
        popup.add(JMenuItem().apply {
            action = SaveAction
            icon = FontIcon.of(FontAwesome.REMOVE, 14, Color.LIGHT_GRAY)
            text = "Remove preferences"
            this@Application.addPropertyChangeListener("preferences-found") { isEnabled = it.newValue as Boolean }
        })

        return popup
    }

    private fun initActions() {
        val contentPane = contentPane as JPanel

        val aMap: ActionMap = contentPane.actionMap
        val iMap: InputMap = contentPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)

        listOf<Action>(
//            ExecuteAction(),
            BrowseAction,
//            SavePropsAction(),
//            RefreshAction()
        )
            .forEach(Consumer { action ->
                aMap.put(action.javaClass, action)

                val accelerator = action.getValue(Action.ACCELERATOR_KEY)

                if (accelerator is KeyStroke) {
                    iMap.put(accelerator, action)
                    println(accelerator)
                }
            })
    }

    private fun findExtensions(dir: File) {
        /* delete all list elements */
        extensionList.clear()

        /* ottiene tutte le sottocartelle */
        var files = dir.listFiles { obj: File -> obj.isDirectory }!!

        /* ottiene tutti i file */
        files = dir.listFiles { obj: File -> obj.isFile }!!

        files.map { file -> getExtension(file) }
            .sorted()
            .groupingBy { it }
            .eachCount()
            .forEach { (extension, frequency) ->
                extensionList.addExtension(extension, frequency)
            }

    }

    fun update(directory: File) {
        Logger.logTime()
        Logger.append("Path set to ")
            .append(directory.path, Logger.defaultStyle.colorize(Color(218, 115, 11)))
            .append("\n")

        currentDirectory = directory

        directoryField.text = currentDirectory.absolutePath

        findExtensions(currentDirectory)

        // search for stored extensions
        with(openSession()) {
            val folder = selectOne<Folder>("select-by-path", currentDirectory.absolutePath)

            if (folder != null) {
                val fileExtensions = selectList<FileExtension>("select-associated-extensions", folder)
                    .map { it.extension }

                extensionList.select(fileExtensions)

                Logger.icon(FontIcon.of(FontAwesome.INFO, 11, Color(172, 107, 121)))
                    .append("Saved extensions found for this folder: ")
                    .append(fileExtensions.toString())
                    .append("\n\n")

                firePropertyChange("preferences-found", false, true)
            } else {
                firePropertyChange("preferences-found", true, false)

                Logger.message("")
            }

            commit()
            close()
        }
    }

    object SelectDirectoryAction : AbstractAction() {
        override fun actionPerformed(evt: ActionEvent) {
            val directory = File(directoryField.text)

            if (directory.exists()) {
                currentDirectory = directory
                directoryField.text = currentDirectory.absolutePath

                update(currentDirectory)
            } else {
                // reset the old path
                directoryField.text = currentDirectory.absolutePath

                System.err.println("Path $directory does not exist")
            }
        }
    }

    object BrowseAction : AbstractAction() {
        init {
            putValue(ACCELERATOR_KEY, getKeyStroke("ctrl O"))
        }

        override fun actionPerformed(actionEvent: ActionEvent) {
            fileChooser.currentDirectory = currentDirectory

            if (fileChooser.showOpenDialog(Application) == JFileChooser.APPROVE_OPTION)
                update(fileChooser.selectedFile)
        }
    }

    object DeleteAction : AbstractAction() {
        init {
            putValue(ACCELERATOR_KEY, getKeyStroke("ctrl M"))
        }

        override fun actionPerformed(e: ActionEvent) {
            val task = DeletionTask(
                directory = currentDirectory,
                extensions = extensionList.getCheckedExtensions().toSet(),
                toTrash = trashCheckBox.isSelected
            )
            task.addPropertyChangeListener {
                if (it.propertyName == "state" && it.newValue == SwingWorker.StateValue.DONE) {
                    findExtensions(currentDirectory)
                }
            }
            task.execute()
        }
    }

    object SaveAction : AbstractAction() {

        private fun getFolder(directory: File): Folder {
            with(openSession()) {
                var folder = selectOne<Folder>("select-by-path", directory.absolutePath)

                // if the path was not found, create a new row in the database
                if (folder == null) {
                    folder = Folder(directory.absolutePath)
                    insert("insert-path", folder)
                }

                commit()
                close()

                return folder
            }
        }

        override fun actionPerformed(e: ActionEvent?) {
            with(openSession()) {
                val folder = getFolder(currentDirectory)
                println(folder.id)

                delete("delete-all-associated-extensions", folder)

                extensionList.getCheckedExtensions().forEach { ext ->
                    var fileExtension = selectOne<FileExtension>("select-by-extension", ext)

                    if (fileExtension == null) {
                        fileExtension = FileExtension(ext)
                        insert("insert", fileExtension)
                    }

                    insert("link-to-folder", mapOf("folder_id" to folder.id, "extension_id" to fileExtension.id))
                }

                commit()
                close()
            }
        }

    }
}

fun randomFiles(n: Int = 10) {
    val extensions = listOf("fd", "ggdf", "j9", "ppo")
    val path = File(System.getProperty("user.dir"), "file_test").toPath()
    val subdirectory = File(path.toFile(), "sub_folder").toPath()
        .apply { if (!Files.exists(this)) Files.createDirectory(this) }

    for (i in 1..10) {
        try {
            Files.createFile(path.resolve("file_${System.currentTimeMillis()}." + extensions.random()))
            Files.createFile(subdirectory.resolve("file_${System.currentTimeMillis()}." + extensions.random()))
        } catch (_: java.nio.file.FileAlreadyExistsException) {

        }
    }
}

