package fr.itlinkshare.server.service

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource

fun hikari(driver: String, jdbcUrl: String): HikariDataSource {
    val config = HikariConfig()
    config.driverClassName = driver
    config.jdbcUrl = jdbcUrl
    config.maximumPoolSize = 3
    config.isAutoCommit = false
    config.transactionIsolation = "TRANSACTION_REPEATABLE_READ"
    config.validate()
    return HikariDataSource(config)
}