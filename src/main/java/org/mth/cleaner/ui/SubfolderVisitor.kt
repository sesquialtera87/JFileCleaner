package org.mth.cleaner.ui

import org.tinylog.Logger
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.FileVisitor
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.BasicFileAttributes
import javax.swing.SwingWorker
import kotlin.io.path.extension
import kotlin.time.measureTime

class SubfolderVisitor(val baseFolder: Path, extensions: Collection<String>) : SwingWorker<Map<String, Int>, Boolean>() {

    val stat: MutableMap<String, Int> = extensions.associate { Pair(it, 0) }.toMutableMap()

    @Suppress("HardCodedStringLiteral")
    override fun doInBackground(): Map<String, Int> {
        Logger.debug { "Initialized stats: $stat" }

        val time = measureTime { Files.walkFileTree(baseFolder, StatVisitor()) }
        Logger.debug { "Stats collected in ${time.inWholeMilliseconds}ms" }
        Logger.debug { "Stats: $stat" }

        return stat
    }

    private inner class StatVisitor : FileVisitor<Path> {

        override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
            return FileVisitResult.CONTINUE
        }

        override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
            val ext = file.extension.uppercase()

            if (stat.containsKey(ext))
                stat[ext] = stat[ext]!! + 1

            return FileVisitResult.CONTINUE
        }

        override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult {
            return FileVisitResult.CONTINUE
        }

        override fun postVisitDirectory(dir: Path, exc: IOException?): FileVisitResult {
            return FileVisitResult.CONTINUE
        }
    }
}