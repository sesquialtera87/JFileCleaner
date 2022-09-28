package org.mth

import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.FileVisitor
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import java.util.*

class DirectoryWalker(
        private val useTrash: Boolean,
        private val responseDigest: (TraversingResponse) -> Unit
) : FileVisitor<Path> {

    lateinit var isCancelled: () -> Boolean
    private val visitedStack: Stack<VisitorItem> = Stack()


    inner class VisitorItem constructor(val path: Path) {

        /* indica se la cartella contiene files */
        var noFiles = true

        /* numero delle sottocartelle presenti
        * Viene usato durante la ricorsione per tenere traccia delle eventuali sottocartelle in principio
        * presenti, ma poi eliminate durante la visita perchè vuote */
        var subfolderCounter = 0

        fun isEmpty() = noFiles && subfolderCounter == 0

        fun getFile() = path.toFile()
    }


    override fun visitFile(p0: Path?, p1: BasicFileAttributes?): FileVisitResult {
        /* mark directory as not empty */
        visitedStack.peek().noFiles = false


        return if (isCancelled.invoke())
            FileVisitResult.TERMINATE
        else
            FileVisitResult.CONTINUE
    }

    override fun visitFileFailed(p0: Path?, p1: IOException?) = FileVisitResult.CONTINUE

    override fun preVisitDirectory(path: Path?, attr: BasicFileAttributes?): FileVisitResult {
        // increment the counter of the subfolder
        if (!visitedStack.empty())
            visitedStack.peek().subfolderCounter++

        visitedStack.push(VisitorItem(path!!))
        println("Visiting directory ${path.fileName}")


        return if (isCancelled.invoke())
            FileVisitResult.TERMINATE
        else
            FileVisitResult.CONTINUE
    }

    override fun postVisitDirectory(p0: Path?, attrs: IOException?): FileVisitResult? {
        val lastSeen = visitedStack.pop()

        with(lastSeen) {
            println("Directory ${path.fileName}:")
            println("\tIs empty: ${isEmpty().toString().toUpperCase()}")
        }

        if (lastSeen.isEmpty()) {
            try {
                /* delete the directory */
                lastSeen.getFile().delete(useTrash)

                responseDigest.invoke(TraversingResponse(
                        path = p0!!,
                        isEmpty = true,
                        visitingLevel = visitedStack.size + 1
                ))

                if (!visitedStack.isEmpty())
                    visitedStack.peek().subfolderCounter--
            } catch (ex: Exception) {
                responseDigest.invoke(TraversingResponse(
                        path = p0!!,
                        isEmpty = true,
                        visitingLevel = visitedStack.size + 1,
                        deletionSucceeded = false
                ))
                ex.run { printStackTrace(System.err) }
            }
        } else responseDigest.invoke(TraversingResponse(
                path = p0!!,
                isEmpty = false,
                visitingLevel = visitedStack.size + 1,
                deletionSucceeded = false
        ))


        return if (isCancelled.invoke())
            FileVisitResult.TERMINATE
        else
            FileVisitResult.CONTINUE
    }
}

/**
 * Oggetto usato per monitorare la visita delle sottocartelle durante l'eliminazione di quelle vuote
 */
data class TraversingResponse(
        val path: Path,
        val isEmpty: Boolean,
        val visitingLevel: Int,
        val deletionSucceeded: Boolean = true
)