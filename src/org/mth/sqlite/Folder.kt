package org.mth.sqlite

class Folder() {

    constructor(path: String) : this() {
        this.path = path
    }

    constructor(path: String, recursive: Boolean) : this() {
        this.path = path
        this.recursive = if (recursive) 1 else 0
    }

    var path: String = ""
    var id: Int = 0
    var recursive: Short = 0

    fun recursionEnabled() = recursive == 1.toShort()
}