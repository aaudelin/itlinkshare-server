package fr.itlinkshare.server.directory

import fr.itlinkshare.server.authentication.model.AccountTable
import fr.itlinkshare.server.directory.model.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

fun ResultRow.toDirectory() = Directory(this[DirectoryTable.id].value, this[DirectoryTable.name])
fun ResultRow.toOrganization() = Organization(this[OrganizationTable.id].value, this[OrganizationTable.name])

fun addOrganization(organization: Organization): Int {
    val id = transaction {
        OrganizationTable.insertAndGetId {
            it[name] = organization.organizationName
        }
    }
    return id.value
}

fun addUserOrganization(userId: Int, organizationId: Int, userRole: String): Int {
    transaction {
        OrganizationUserTable.insert {
            it[organization] = organizationId
            it[user] = userId
            it[role] = userRole
        }
    }
    return 1
}

fun findUserOrganizations(login: String): List<Organization> {
    return transaction {
        OrganizationTable.join(OrganizationUserTable, JoinType.INNER, additionalConstraint = {OrganizationUserTable.organization eq OrganizationTable.id})
            .join(AccountTable, JoinType.INNER, additionalConstraint = {AccountTable.id eq OrganizationUserTable.user and (AccountTable.login eq login)})
            .slice(OrganizationTable.id, OrganizationTable.name)
            .selectAll()
            .map { it.toOrganization()}
    }
}

fun addDirectory(directoryName: String, organizationId: Int): Int {
    val id = transaction {
        DirectoryTable.insertAndGetId {
            it[name] = directoryName
            it[organization] = organizationId
        }
    }
    return id.value
}

fun findOrganizationDirectories(organizationId: Int): List<Directory> {
    return transaction {
        DirectoryTable.select {
            DirectoryTable.organization eq organizationId
        }.map { it.toDirectory() }
    }
}

fun findUserOrganizationRight(organizationId: Int, login: String): String {
    val result = transaction {
        OrganizationUserTable
            .join(AccountTable, JoinType.INNER, additionalConstraint = {AccountTable.id eq OrganizationUserTable.user and (AccountTable.login eq login)})
            .select {OrganizationUserTable.organization eq organizationId}
            .map { it[OrganizationUserTable.role] }
    }
    return result[0]
}
