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
        server ?: throw IllegalArgumentException("Server cannot be null")
        server.url ?: throw IllegalArgumentException("Server url cannot be null")
        val trimmedUrl = server.url.trimEnd('/')

        if (urlsSet.contains(trimmedUrl)) {
            throw IllegalArgumentException("Server url [${server.url}] is not unique")
        }
        urlsSet.add(trimmedUrl)

        val credentials = server.credentials
        if (credentials != null) {
            credentials.login ?: throw IllegalArgumentException("Login cannot be null")
            credentials.password ?: throw IllegalArgumentException("Password cannot be null")
        }
    }
}