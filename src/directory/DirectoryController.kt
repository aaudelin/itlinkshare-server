package fr.itlinkshare.server.directory

import fr.itlinkshare.server.AuthorizationException
import fr.itlinkshare.server.InvalidAccountException
import fr.itlinkshare.server.authentication.createTokenResponse
import fr.itlinkshare.server.authentication.findAccount
import fr.itlinkshare.server.directory.model.*
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.features.BadRequestException
import io.ktor.features.NotFoundException
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.*

fun Route.directoryManagement() {
    authenticate {
        post("/organization") {
            val organization = call.receive<Organization>()
            val token = createTokenResponse(call)
            val account = findAccount(token.login) ?: throw InvalidAccountException("Account does not exist")

            val organizationId = addOrganization(organization)
            addUserOrganization(account.id!!, organizationId, DirectoryRight.ADMIN.name)

            call.response.status(HttpStatusCode.Created)
            call.respond(Organization(organizationId, organization.organizationName))
        }

        get("/organization") {
            val token = createTokenResponse(call)
            val account = findAccount(token.login) ?: throw InvalidAccountException("Account does not exist")

            val organizations = findUserOrganizations(account.login.login)

            call.response.status(HttpStatusCode.Accepted)
            call.respond(organizations)
        }

        put("/organization/{organizationId}") {
            val organization = call.receive<Organization>()
            val organizationId = call.parameters["organizationId"]?.toInt() ?: throw BadRequestException("Organization ID is not a valid Int")
            val token = createTokenResponse(call)

            val right = findUserOrganizationRight(organizationId, token.login)
            if (DirectoryRight.ADMIN.name != right) throw AuthorizationException()

            if (updateOrganization(organization) <= 0) {
                throw NotFoundException("Organization $organizationId does not exist, impossible to update")
            }

            call.response.status(HttpStatusCode.Accepted)
            call.respond(Organization(organizationId, organization.organizationName))
        }

        get("/organization/{organizationId}") {
            val organizationId = call.parameters["organizationId"]?.toInt() ?: throw BadRequestException("Organization ID is not a valid Int")

            val token = createTokenResponse(call)
            findUserOrganizationRight(organizationId, token.login) ?: throw AuthorizationException()

            val organization = findOrganization(organizationId)
            val directories = findOrganizationDirectories(organizationId)
            val userRoles = findOrganizationUsers(organizationId)

            call.response.status(HttpStatusCode.Accepted)
            call.respond(OrganizationFull(organization, directories, userRoles))
        }

        delete("/organization/{organizationId}") {
            val organizationId = call.parameters["organizationId"]?.toInt() ?: throw BadRequestException("Organization ID is not a valid Int")
            val token = createTokenResponse(call)

            val right = findUserOrganizationRight(organizationId, token.login)
            if (DirectoryRight.ADMIN.name != right) throw AuthorizationException()

            if (deleteOrganization(organizationId) <= 0) {
                throw NotFoundException("Organization $organizationId does not exist, impossible to delete")
            }
        }

        post("/organization/{organizationId}/user") {
            val (login, role) = call.receive<UserRole>()
            val token = createTokenResponse(call)
            val organizationId = call.parameters["organizationId"]?.toInt() ?: throw BadRequestException("Organization ID is not a valid Int")

            val right = findUserOrganizationRight(organizationId, token.login)
            if (DirectoryRight.ADMIN.name != right) throw AuthorizationException()

            val accountAdded = findAccount(login) ?: throw InvalidAccountException("Requested added account $login")

            addUserOrganization(accountAdded.id!!, organizationId, DirectoryRight.valueOf(role).name)

            call.response.status(HttpStatusCode.Created)
            call.respond(UserRole(login, role))
        }

        delete("/organization/{organizationId}/user/{userLogin}") {
            val token = createTokenResponse(call)
            val organizationId = call.parameters["organizationId"]?.toInt() ?: throw BadRequestException("Organization ID is not a valid Int")
            val userLogin = call.parameters["userLogin"] ?: throw BadRequestException("User Login is not a valid String")

            val right = findUserOrganizationRight(organizationId, token.login)
            if (DirectoryRight.ADMIN.name != right) throw AuthorizationException()

            val accountDeleted = findAccount(userLogin) ?: throw InvalidAccountException("Requested deleted account $userLogin")

            if (accountDeleted.id == null || deleteUserOrganization(organizationId, accountDeleted.id) <= 0) {
                throw NotFoundException("Organization $organizationId or user $userLogin does not exist, impossible to delete")
            }
        }

        post("/organization/{organizationId}/directory") {
            val directory = call.receive<Directory>()
            val token = createTokenResponse(call)
            val organizationId = call.parameters["organizationId"]?.toInt()
                ?: throw BadRequestException("Organization ID is not a valid Int")

            val right = findUserOrganizationRight(organizationId, token.login)

            if (DirectoryRight.ADMIN.name != right) throw AuthorizationException()
            val directoryId = addDirectory(directory.folderName, organizationId)

            call.response.status(HttpStatusCode.Created)
            call.respond(Directory(directoryId, directory.folderName))
        }

        put("/organization/{organizationId}/directory/{directoryId}") {
            val directory = call.receive<Directory>()
            val token = createTokenResponse(call)
            val organizationId = call.parameters["organizationId"]?.toInt() ?: throw BadRequestException("Organization ID is not a valid Int")
            val directoryId = call.parameters["directoryId"]?.toInt() ?: throw BadRequestException("Directory ID is not a valid Int")

            findUserOrganizationRight(organizationId, token.login) ?: throw AuthorizationException()

            if (updateDirectory(directory) <= 0) {
                throw NotFoundException("Directory $directoryId does not exist, impossible to update")
            }

            call.response.status(HttpStatusCode.Accepted)
            call.respond(Directory(directoryId, directory.folderName))
        }

        delete("/organization/{organizationId}/directory/{directoryId}") {
            val token = createTokenResponse(call)
            val organizationId = call.parameters["organizationId"]?.toInt() ?: throw BadRequestException("Organization ID is not a valid Int")
            val directoryId = call.parameters["directoryId"]?.toInt() ?: throw BadRequestException("Directory ID is not a valid Int")

            val right = findUserOrganizationRight(organizationId, token.login)
            if (DirectoryRight.ADMIN.name != right) throw AuthorizationException()

            if (deleteDirectory(directoryId) <= 0) {
                throw NotFoundException("Directory $directoryId does not exist, impossible to delete")
            }
        }
    }
}