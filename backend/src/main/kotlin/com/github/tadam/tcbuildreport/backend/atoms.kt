package com.github.tadam.tcbuildreport.backend

import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.util.Date

// prefixed by "A" to avoid teamcity-rest-client types
typealias ABuildId = String
typealias ABuildIdList = List<ABuildId>
typealias ServerUrl = String

// jackson-module-kotlin doesn't always help to transform to transform
// Kotlin data types to/from JSON, so additional annotations are required
//
// See https://github.com/redisson/redisson/issues/1265
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
data class ABuild(
        val server: String,
        val id: ABuildId,
        val buildTypeId: String,
        val buildNumber: String,
        val startDate: Date,
        val webUrl: String)

data class Server(val url: ServerUrl, val credentials: Credentials? = null)
data class Servers(val servers: List<Server>)
data class Credentials(val login: String, val password: String)