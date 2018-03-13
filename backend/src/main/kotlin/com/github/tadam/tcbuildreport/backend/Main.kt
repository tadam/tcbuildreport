package com.github.tadam.tcbuildreport.backend

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.gson.gson
import io.ktor.locations.*
import io.ktor.response.*
import io.ktor.routing.*

@Location("/api/ping")
class Ping()

data class Pong(val data: String)

fun Application.main() {
    install(DefaultHeaders)
    install(Compression)
    install(Locations)
    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
        }
    }

    routing {
        get<Ping> {
            call.respond(Pong("pong"))
        }
    }
}