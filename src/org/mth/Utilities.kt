package org.mth

import com.sun.jna.Platform
import com.sun.jna.platform.FileUtils
import java.awt.Color
import java.io.File
import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*
import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.SwingUtilities
import javax.swing.text.MutableAttributeSet
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.Style
import javax.swing.text.StyleConstants


class MyVisitor(
    private val useTrash: Boolean,
    private val responseDigest: (TraversingResponse) -> Unit,
) : FileVisitor<Path> {

    inner class VisitorItem constructor(val path: Path) {
        /* indica se la cartella contiene files */
        var noFiles = true

        /* numero delle sottocartelle presenti
        * Viene usato durante la ricorsione per tenere traccia delle eventuali sottocartelle in principio
        * presenti, ma poi eliminate durante la visita perchè vuote */
        var subfolderCounter = 0

        fun isEmpty() = noFiles && subfolderCounter == 0
    }

    private val directoryVisited: Stack<VisitorItem> = Stack()

    override fun visitFile(p0: Path?, p1: BasicFileAttributes?): FileVisitResult {
        /* mark directory as not empty */
        directoryVisited.peek().noFiles = false

        return FileVisitResult.CONTINUE
    }

    override fun visitFileFailed(p0: Path?, p1: IOException?) = FileVisitResult.CONTINUE

    override fun preVisitDirectory(path: Path?, attr: BasicFileAttributes?): FileVisitResult {
        // increment the counter of the subfolder
        if (!directoryVisited.empty())
            directoryVisited.peek().subfolderCounter++

        directoryVisited.push(VisitorItem(path!!))
        println("Visiting directory ${path.fileName}")

        return FileVisitResult.CONTINUE
    }

    override fun postVisitDirectory(p0: Path?, attrs: IOException?): FileVisitResult? {
        val lastSeen = directoryVisited.pop()

        with(lastSeen) {
            println("Directory ${path.fileName}:")
            println("\tIs empty: ${isEmpty().toString().toUpperCase()}")
        }

        if (lastSeen.isEmpty()) {
            try {
                /* delete the directory */
                lastSeen.path.toFile().delete(useTrash)

                responseDigest.invoke(
                    TraversingResponse(
                        path = p0!!,
                        isEmpty = true,
                        visitingLevel = directoryVisited.size + 1
                    )
                )

                if (!directoryVisited.isEmpty())
                    directoryVisited.peek().subfolderCounter--
            } catch (ex: Exception) {
                responseDigest.invoke(
                    TraversingResponse(
                        path = p0!!,
                        isEmpty = true,
                        visitingLevel = directoryVisited.size + 1,
                        deletionSucceeded = false
                    )
                )
                ex.run { printStackTrace(System.err) }
            }
        } else responseDigest.invoke(
            TraversingResponse(
                path = p0!!,
                isEmpty = false,
                visitingLevel = directoryVisited.size + 1,
                deletionSucceeded = false
            )
        )

        return FileVisitResult.CONTINUE
    }
}


private val LINUX_TRASH = File(
    System.getProperty("user.home")
            + "/.local/share/Trash/files"
)


fun hasTrash(): Boolean {
    val hasTrash = FileUtils.getInstance().hasTrash()

    if (hasTrash) {
        // se viene riconosciuto il cestino, usa il metodo di FileUtils per eliminare
        deletingFunction = { f ->
            try {
                FileUtils.getInstance().moveToTrash(f)
                true
            } catch (ex: IOException) {
                ex.printStackTrace(System.err)
                false
            }
        }
    } else {
        if (Platform.isLinux() && LINUX_TRASH.exists()) {
            deletingFunction = { file ->
                try {
                    Files.move(
                        file.toPath(),
                        File(LINUX_TRASH, file.name).toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                    )
                    true
                } catch (ex: IOException) {
                    ex.printStackTrace(System.err)
                    false
                }
            }

            return true
        }
    }

    return hasTrash
}

/*
Rimuove un file senza spostarlo nel Cestino
 */
private val standardDeletingFunction = fun(file: File): Boolean {
    try {
        Files.delete(file.toPath())
    } catch (ex: Exception) {
        return false
    }

    return true
}

fun clearEmptySubfolders(dir: File, useTrash: Boolean, responseDigest: (TraversingResponse) -> Unit = {}) {
    Files.walkFileTree(dir.toPath(), MyVisitor(useTrash, responseDigest))
}

fun getTopWindow(component: JComponent): Optional<JFrame> {
    var parent = component.parent
    var found = false

    while (parent != null) {
        if (parent is JFrame) {
            found = true
            break
        } else
            parent = parent.parent
    }

    return if (found)
        Optional.ofNullable(parent as JFrame)
    else
        Optional.empty<JFrame>()
}

/**
 * Se il SO lo consente, elimina un file spostandolo nel Cestino.
 */
private var deletingFunction: (File) -> Boolean = standardDeletingFunction


fun getExtension(file: File): String {
    return if (file.isDirectory) ""
    else
        file.name.substring(file.name.lastIndexOf('.') + 1).toLowerCase()
}

/**
 * Rimuove il file specificato.
 * @author mattia marelli
 * @return TRUE se l'eliminazione del file è avvenuta con successo, FALSE altrimenti
 * @see deletingFunction
 */
fun File.delete(useTrash: Boolean) = if (useTrash)
    deletingFunction.invoke(this)
else standardDeletingFunction.invoke(this)


fun File.extension() = getExtension(this)

fun formattedTimeStamp(): String {
    return LocalDateTime.now().format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM))
}

/**
 * Shortcut for [SwingUtilities.invokeLater]
 */
fun execute(runnable: Runnable) {
    SwingUtilities.invokeLater { runnable.run() }
}


fun Style.italic(): MutableAttributeSet {
    val newStyle = SimpleAttributeSet(this)
    StyleConstants.setItalic(newStyle, true)

    return newStyle
}

fun Style.bold(): MutableAttributeSet {
    val newStyle = SimpleAttributeSet(this)
    StyleConstants.setBold(newStyle, true)

    return newStyle
}

fun Style.colorize(color: Color): SimpleAttributeSet {
    val style = SimpleAttributeSet(this)
    StyleConstants.setForeground(style, color)
    return style
}

