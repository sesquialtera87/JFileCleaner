package org.mth.sqlite

import org.mth.gui.JExtensionList
import org.apache.ibatis.io.Resources
import org.apache.ibatis.session.SqlSession
import org.apache.ibatis.session.SqlSessionFactory
import org.apache.ibatis.session.SqlSessionFactoryBuilder
import java.io.File
import java.util.*


//val DB = File("C:\\Users\\utente\\Documents\\HeidiSQL\\127.0.0.1.sqlite")
val DB = File("org/mth/sqlite/database.sqlite")
var sqlSessionFactory: SqlSessionFactory? = null

fun openSession(): SqlSession = sqlSessionFactory!!.openSession()


fun connectToPreferencesDatabase() {
    if (sqlSessionFactory == null) {
        val props = Properties()
//        props.setProperty("url", "jdbc:sqlite:" + DB::class.java.getResource(DB.DB_RELATIVE_PATH)!!.path)
        props.setProperty("url", "jdbc:sqlite:" + "src/org/mth/sqlite/database.sqlite")

        val inputStream =
            Resources.getResourceAsStream(JExtensionList::class.java.classLoader, "org/mth/sqlite/config.xml")
        sqlSessionFactory = SqlSessionFactoryBuilder().build(inputStream, props)
    } else println("Already connected")
}

fun main() {
    connectToPreferencesDatabase()

    val session = openSession()
    session.selectList<String>("select-all-paths").forEach { println(it) }
    session.selectList<Folder>("select-all-folders").forEach { println(it.id) }
    println(session.selectOne<FileExtension>("select-by-extension", "jpg"))
//    val composer1 = session.selectList<Composer1>(SQLConstants.COMPOSERS_SELECT_ALL).first()
//    println(composer1)
//    println(composer1.imslp)
//    session.update(SQLConstants.COMPOSERS_UPDATE,composer1)

    session.commit()
    session.close()
}