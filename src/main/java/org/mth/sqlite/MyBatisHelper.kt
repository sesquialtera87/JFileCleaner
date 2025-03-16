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

package org.mth.sqlite

import org.apache.ibatis.io.Resources
import org.apache.ibatis.session.SqlSession
import org.apache.ibatis.session.SqlSessionFactory
import org.apache.ibatis.session.SqlSessionFactoryBuilder
import org.jetbrains.annotations.NonNls
import org.mth.cleaner.ApplicationContext
import org.sqlite.JDBC
import org.tinylog.Logger
import java.nio.file.Path
import java.sql.DriverManager
import java.sql.SQLException
import java.util.*
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists


var sqlSessionFactory: SqlSessionFactory? = null

@NonNls
val DB_PATH: String =
    if (ApplicationContext.DEBUG) "C:\\Users\\matti\\OneDrive\\Documenti\\Java\\JFileCleaner\\src\\main\\resources\\org\\mth\\sqlite\\db.sqlite"
    else ApplicationContext.APP_FOLDER.resolve("db.sqlite").normalize().toString()

fun openSession(): SqlSession = sqlSessionFactory!!.openSession()

@Suppress("HardCodedStringLiteral")
fun connectToHistoryDatabase() {
    newSqliteDB(Path(DB_PATH))

    if (sqlSessionFactory == null) {
        val props = Properties().apply {
            setProperty("url", "jdbc:sqlite:$DB_PATH")
        }

        val inputStream = Resources.getResourceAsStream(ApplicationContext::class.java.classLoader, "org/mth/sqlite/my-batis-config.xml")
        sqlSessionFactory = SqlSessionFactoryBuilder().build(inputStream, props)

        createTables()
    } else Logger.debug { "Already connected" }
}

@Suppress("HardCodedStringLiteral")
fun createTables() {
    openSession().run {
        selectOne<Void>("create-deletions")
        selectOne<Void>("view-detailed-time")
        close()
    }
}

@Suppress("HardCodedStringLiteral")
fun newSqliteDB(path: Path) {
    if (path.exists()) {
        Logger.debug { "Database already exists" }
        return
    }

    val url = "jdbc:sqlite:" + path.normalize().absolutePathString()

    try {
        DriverManager.registerDriver(JDBC())
        DriverManager.getConnection(url).use { conn ->
            if (conn != null) {
                val meta = conn.metaData
                Logger.debug { "The driver name is " + meta.driverName }
                Logger.info { "A new database has been created: $path" }
            }
        }
    } catch (e: SQLException) {
        Logger.error(e)
    }
}