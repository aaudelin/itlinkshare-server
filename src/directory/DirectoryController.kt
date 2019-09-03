package fr.itlinkshare.server.directory

import fr.itlinkshare.server.AuthorizationException
import fr.itlinkshare.server.InvalidAccountException
import fr.itlinkshare.server.authentication.Account
import fr.itlinkshare.server.authentication.createTokenResponse
import fr.itlinkshare.server.authentication.findAccount
import fr.itlinkshare.server.directory.model.Directory
import fr.itlinkshare.server.directory.model.DirectoryRight
import fr.itlinkshare.server.directory.model.Organization
import fr.itlinkshare.server.directory.model.UserRole
import io.ktor.application.call
import io.ktor.auth.authenticate
import io.ktor.features.BadRequestException
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.post

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

        post("/organization/{organizationId}/addUser") {
            val (login, role) = call.receive<UserRole>()
            val token = createTokenResponse(call)
            val organizationId = call.parameters["organizationId"]?.toInt()
                ?: throw BadRequestException("Organization ID is not a valid Int")

            val right = findUserOrganizationRight(organizationId, token.login)
            if (DirectoryRight.ADMIN.name == right) throw AuthorizationException()

            val accountAdded = findAccount(token.login) ?: throw InvalidAccountException("Requested added account $login")

            addUserOrganization(accountAdded.id!!, organizationId, DirectoryRight.valueOf(role).name)

            call.response.status(HttpStatusCode.Created)
            call.respond(UserRole(login, role))
        }

        post("/organization/{organizationId}/directory") {
            val directory = call.receive<Directory>()
            val token = createTokenResponse(call)
            val organizationId = call.parameters["organizationId"]?.toInt()
                ?: throw BadRequestException("Organization ID is not a valid Int")

            val right = findUserOrganizationRight(organizationId, token.login)

            if (DirectoryRight.ADMIN.name == right) throw AuthorizationException()
            val directoryId = addDirectory(directory.folderName, organizationId)

            call.response.status(HttpStatusCode.Created)
            call.respond(Directory(directoryId, directory.folderName))
        }
    }
}