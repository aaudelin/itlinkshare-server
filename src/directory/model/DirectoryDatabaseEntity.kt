package fr.itlinkshare.server.directory.model

import org.jetbrains.exposed.dao.IntIdTable
import org.jetbrains.exposed.sql.Table

object DirectoryTable: IntIdTable() {
    val name = varchar("name", 50)
    val organization = integer("organization_id")
}

object OrganizationTable: IntIdTable() {
    val name = varchar("name", 50)
}

object OrganizationUserTable: Table() {
    val organization = integer("organization_id")
    val user = integer("user_id")
    val role = varchar("role", 50)
}
