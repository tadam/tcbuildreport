package com.github.tadam.tcbuildreport.backend

import io.ktor.application.*
import io.ktor.http.ContentType
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

import org.jetbrains.teamcity.rest.TeamCityInstanceFactory

fun main(args: Array<String>) {
    val server = embeddedServer(Netty, port = 8080) {
        routing {
            get("/") {
                val builds = TeamCityInstanceFactory.guestAuth("https://teamcity.jetbrains.com")
                        .queuedBuilds()
                call.respondText(builds.toString(), ContentType.Text.Plain)
            }
        }
    }
    server.start(wait = true)
}