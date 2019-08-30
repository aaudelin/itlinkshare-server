package fr.itlinkshare.server.authentication

import fr.itlinkshare.server.authentication.model.AccountTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction

fun ResultRow.toAccount() = Account(Login(this[AccountTable.login], this[AccountTable.password]))

fun addAccount(account: Account): Int {
    transaction {
        AccountTable.insert {
            it[login] = account.login.login
            it[password] = account.login.password
        }
    }
    return 1
}

fun findAccount(login: String): Account? {
    return transaction {
        val listResults = AccountTable.select {
            AccountTable.login eq login
        }.map { it.toAccount() }

        if (listResults.isEmpty()) null else listResults.first()
    }
}

fun deleteAccount(login: String): Int {
    return transaction {
        AccountTable.deleteWhere {
            AccountTable.login eq login
        }
    }
}
