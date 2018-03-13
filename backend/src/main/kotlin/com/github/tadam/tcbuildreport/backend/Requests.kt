package com.github.tadam.tcbuildreport.backend

import io.ktor.locations.Location

@Location("/ping")
class Ping()

@Location("/builds")
data class BuildsParams(val offset: Int = 0, val limit: Int = 10,
                        val sortBy: SortBy = SortBy.server,
                        val sortOrder: SortOrder = SortOrder.asc)

enum class SortBy {
    server, startDate
}

enum class SortOrder {
    asc, desc
}

data class Servers(val servers: List<Server>)
data class Server(val url: String, val credentials: Credentials? = null)
data class Credentials(val login: String, val password: String)

/* In addition to uniqueness check of server urls, here we also check
   that servers.servers is not null (!), etc.
   This is caused by strange behaviour of JSON converter from Ktor.
   If we send the following JSON:
   { "servers": null }
   it doesn't throw any exception and returned value res.servers is null!
   Same for nested values as well.
 */
fun validateRequestServers(s: Servers) {
    val urlsSet = mutableSetOf<String>()

    s.servers ?: throw IllegalArgumentException("List of servers cannot be null")
    for (server in s.servers) {
        server.url ?: throw IllegalArgumentException("Server url cannot be null")

        if (urlsSet.contains(server.url)) {
            throw IllegalArgumentException("Server urls should be unique")
        }
        urlsSet.add(server.url)

        val credentials = server.credentials
        if (credentials != null) {
            credentials.login ?: throw IllegalArgumentException("Login cannot be null")
            credentials.password ?: throw IllegalArgumentException("Password cannot be null")
        }
    }
}