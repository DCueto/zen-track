package me.dcueto.zentrackapp.db

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database

object DatabaseFactory {
    fun init(application: Application) {
        val cfg = application.environment.config
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = cfg.property("database.url").getString()
            driverClassName = cfg.property("database.driver").getString()
            username = cfg.property("database.user").getString()
            password = cfg.property("database.password").getString()
            maximumPoolSize = cfg.propertyOrNull("database.maxPoolSize")?.getString()?.toInt() ?: 10
        }
        val dataSource = HikariDataSource(hikariConfig)

        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load()
            .migrate()

        Database.connect(dataSource)
    }
}
