package fr.itlinkshare.server.authentication

data class JwtTokenConfig(val issuer: String, val audience: String, val realm: String)

data class JwtTokenResponse(val login: String, val token: String, val expirationDate: String)

data class Login(var login: String, var password: String)

data class LoginConfirm(val login: String, val password: String, val confirmPassword: String)

data class Account(val id: Int?, val login: Login)