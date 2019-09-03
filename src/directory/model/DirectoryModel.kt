package fr.itlinkshare.server.directory.model

enum class DirectoryRight {
    ADMIN, CONTRIBUTOR
}
data class Organization(val id: Int?, var organizationName: String)

data class Directory(val id: Int?, var folderName: String)

data class UserRole(val login: String, val role: String)