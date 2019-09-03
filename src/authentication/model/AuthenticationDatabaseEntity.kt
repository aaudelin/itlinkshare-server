package fr.itlinkshare.server.authentication.model

import org.jetbrains.exposed.dao.IntIdTable

object AccountTable: IntIdTable() {
    val login = varchar("login", 50)
    val password = varchar("password", 255)
}