package com.github.tadam.tcbuildreport.backend

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.gson.gson
import io.ktor.response.*
import io.ktor.routing.*

data class Pong(val data: String)

fun Application.main() {
    install(DefaultHeaders)
    install(Compression)
    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
        }
    }

    routing {
        get("/api/ping") {
            call.respond(Pong("pong"))
        }
    }
}