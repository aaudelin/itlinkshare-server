package fr.itlinkshare.server.authentication

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import fr.itlinkshare.server.InvalidAccountException
import fr.itlinkshare.server.InvalidTokenException
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.authenticate
import io.ktor.auth.authentication
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.jwt.jwt
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.request.header
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.*
import io.ktor.util.getDigestFunction
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

const val HOUR_TO_MILLIS = 1000*60*60
fun formatDate(date: Date): String = SimpleDateFormat("yyyy/MM/dd hh:mm:ss").format(date)

fun Route.login(jwtTokenConfig: JwtTokenConfig) {
    application.install(Authentication) {
        jwt {
            verifier(makeJwtVerifier(jwtTokenConfig.issuer, jwtTokenConfig.audience))
            this.realm = realm
            validate { credential ->
                if (credential.payload.audience.contains(jwtTokenConfig.audience))
                    JWTPrincipal(credential.payload)
                else
                    null
            }
        }
    }

    authenticate {
        get("/authentication/verify") {
            call.respond(createTokenResponse(call))
        }

        delete("/authentication/login") {
            val token = createTokenResponse(call)
            val (login, password) = call.receive<Login>()

            val account = findAccount(token.login) ?: throw InvalidAccountException("Account does not exist")
            if (account.login.login != login && account.login.password != digestFunction(password).joinToString("")) throw InvalidAccountException("Invalid credentials")

            deleteAccount(login)

            call.respond("Account $login successfully deleted")
        }
    }

    post("/authentication/login") {
        val (login, password) = call.receive<Login>()

        val account = findAccount(login) ?: throw InvalidAccountException("Account does not exist")
        if (account.login.password != digestFunction(password).joinToString("")) throw InvalidAccountException("Invalid password")

        val expiresAt = Date.from(Instant.now().plusMillis(HOUR_TO_MILLIS.toLong()))
        val token = JWT.create()
            .withSubject(login)
            .withIssuer(jwtTokenConfig.issuer)
            .withAudience(jwtTokenConfig.audience)
            .withExpiresAt(expiresAt)
            .sign(algorithm)
        call.respond(JwtTokenResponse(login, token, formatDate(expiresAt)))
    }

    post("/authentication/signup") {
        val (login, password, confirmPassword) = call.receive<LoginConfirm>()
        if (findAccount(login) != null) throw InvalidAccountException("Login already exists")
        if (password != confirmPassword) throw InvalidAccountException("Password and confirmation are different")

        addAccount(Account(Login(login, digestFunction(password).joinToString(""))))

        call.response.status(HttpStatusCode.Created)
        call.respond("Account creation successful for $login")
    }

}


private fun createTokenResponse(call: ApplicationCall): JwtTokenResponse {
    val token = call.request.header(HttpHeaders.Authorization)?.removePrefix("Bearer ") ?: throw InvalidTokenException("Invalid token")
    val principal = call.authentication.principal<JWTPrincipal>()
    val payload = principal?.payload ?: throw InvalidTokenException("Empty token payload")
    val subjectString = payload.subject?.removePrefix("auth0|") ?: throw InvalidTokenException("Invalid subject")
    val dueDate = payload.expiresAt ?: throw InvalidTokenException("Invalid expires date")
    return JwtTokenResponse(subjectString, token, formatDate(dueDate))
}
private val digestFunction = getDigestFunction("SHA-256") { SECRET+"${it.length}" }
private val algorithm = Algorithm.HMAC256(SECRET)
fun makeJwtVerifier(issuer: String, audience: String): JWTVerifier = JWT
    .require(algorithm)
    .withAudience(audience)
    .withIssuer(issuer)
    .build()
