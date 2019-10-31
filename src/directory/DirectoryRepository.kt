package fr.itlinkshare.server.directory

import fr.itlinkshare.server.authentication.model.AccountTable
import fr.itlinkshare.server.directory.model.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

fun ResultRow.toDirectory() = Directory(this[DirectoryTable.id].value, this[DirectoryTable.name])
fun ResultRow.toOrganization() = Organization(this[OrganizationTable.id].value, this[OrganizationTable.name])
fun ResultRow.toUserRole() = UserRole(this[AccountTable.login], this[OrganizationUserTable.role])

fun addOrganization(organization: Organization): Int {
    val id = transaction {
        OrganizationTable.insertAndGetId {
            it[name] = organization.organizationName
        }
    }
    return id.value
}

fun updateOrganization(organization: Organization): Int {
    return transaction {
        OrganizationTable.update(
            {
                OrganizationTable.id eq organization.id
            }
        ) {
            it[name] = organization.organizationName
        }
    }
}

fun findOrganization(organizationId: Int): Organization {
    return transaction {
        OrganizationTable.select {
            OrganizationTable.id eq organizationId
        }.first()
        .let { it.toOrganization() }
    }
}

fun updateDirectory(directory: Directory): Int {
    return transaction {
        DirectoryTable.update(
            {
                DirectoryTable.id eq directory.id
            }
        ) {
            it[name] = directory.folderName
        }
    }
}

fun deleteDirectory(directoryId: Int): Int {
    return transaction {
        DirectoryTable.deleteWhere {
                DirectoryTable.id eq directoryId
            }
    }
}

fun deleteOrganization(organizationId: Int): Int {
    return transaction {
        OrganizationTable.deleteWhere {
            OrganizationTable.id eq organizationId
        }
    }
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

fun deleteUserOrganization(userId: Int, organizationId: Int): Int {
    return transaction {
        OrganizationUserTable.deleteWhere {
            OrganizationUserTable.user eq userId and (OrganizationUserTable.organization eq organizationId)
        }
    }
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

fun findOrganizationUsers(organizationId: Int): List<UserRole> {
    return transaction {
        OrganizationUserTable
            .join(AccountTable, JoinType.INNER, additionalConstraint = {OrganizationUserTable.user eq AccountTable.id})
            .slice(AccountTable.login, OrganizationUserTable.role)
            .selectAll()
            .map { it.toUserRole() }
    }
}

fun findUserOrganizationRight(organizationId: Int, login: String): String? {
    val result = transaction {
        OrganizationUserTable
            .join(AccountTable, JoinType.INNER, additionalConstraint = {AccountTable.id eq OrganizationUserTable.user and (AccountTable.login eq login)})
            .select {OrganizationUserTable.organization eq organizationId}
            .map { it[OrganizationUserTable.role] }
    }
    return result[0]
}
