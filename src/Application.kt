package fr.itlinkshare.server

import com.fasterxml.jackson.databind.SerializationFeature
import fr.itlinkshare.server.authentication.JwtTokenConfig
import fr.itlinkshare.server.authentication.login
import fr.itlinkshare.server.service.hikari
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CORS
import io.ktor.features.ContentNegotiation
import io.ktor.features.StatusPages
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.jackson.jackson
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import org.jetbrains.exposed.sql.Database

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {
    install(CORS) {
        method(HttpMethod.Options)
        method(HttpMethod.Put)
        method(HttpMethod.Get)
        method(HttpMethod.Delete)
        header(HttpHeaders.Authorization)
        header(HttpHeaders.ContentType)
        allowCredentials = true
        anyHost() // @TODO: Don't do this in production if possible. Try to limit it.
    }

    install(ContentNegotiation) {
        jackson {
            enable(SerializationFeature.INDENT_OUTPUT)
        }
    }

    install(StatusPages) {
        exception<AuthenticationException> { cause ->
            call.respond(HttpStatusCode.Unauthorized)
        }
        exception<AuthorizationException> { cause ->
            call.respond(HttpStatusCode.Forbidden)
        }
        exception<InvalidTokenException> { cause ->
            call.respond(HttpStatusCode.Unauthorized, cause)
        }
    }

    initDatabase();
    var jwtTokenConfig = initJwtTokenConfig()

    routing {
        get("/status") {
            call.respondText("Server is running : OK", contentType = ContentType.Text.Plain)
        }
        login(jwtTokenConfig)


    }
}

fun Application.initDatabase() {
    var driver = environment.config.property("database.driver").getString()
    var jdbcUrl = environment.config.property("database.url").getString()
    Database.connect(hikari(driver, jdbcUrl))
}

fun Application.initJwtTokenConfig(): JwtTokenConfig {
    val issuer = environment.config.property("jwt.domain").getString()
    val audience = environment.config.property("jwt.audience").getString()
    val realm = environment.config.property("jwt.realm").getString()
    return JwtTokenConfig(issuer, audience, realm)
}

class InvalidTokenException(cause: String) : RuntimeException(cause)
class AuthenticationException : RuntimeException()
class AuthorizationException : RuntimeException()

