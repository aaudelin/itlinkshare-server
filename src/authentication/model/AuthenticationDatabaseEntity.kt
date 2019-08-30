package fr.itlinkshare.server.authentication.model

import org.jetbrains.exposed.sql.Table

object AccountTable: Table() {
    val id = integer("id").autoIncrement().primaryKey()
    val login = varchar("login", 50)
    val password = varchar("password", 255)
}