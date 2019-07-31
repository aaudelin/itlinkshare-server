package fr.itlinkshare.server.authentication

data class JwtTokenConfig(val issuer: String, val audience: String, val realm: String)

data class JwtTokenResponse(val login: String, val token: String, val expirationDate: String)

data class Login(val login: String, val password: String)