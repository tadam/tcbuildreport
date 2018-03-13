package com.github.tadam.tcbuildreport.backend

import com.google.gson.JsonSyntaxException
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.*
import io.ktor.gson.gson
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.locations.Locations
import io.ktor.locations.get
import io.ktor.locations.post
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.route
import io.ktor.routing.routing

fun Application.main() {
    install(DefaultHeaders)
    install(Compression)
    install(Locations)
    install(CORS) {
        anyHost()
        method(HttpMethod.Get)
        method(HttpMethod.Post)
    }
    install(ContentNegotiation) {
        gson {
            setPrettyPrinting()
        }
    }
    install(StatusPages) {
        exception<IllegalArgumentException> {
            respondBadRequest(call, it.message)
        }
        exception<NoSuchElementException> {
            respondBadRequest(call, it.message)
        }
        exception<JsonSyntaxException> {
            respondBadRequest(call, it.message)
        }
    }

    routing {
        route("/api") {
            get<Ping> {
                call.respond(PingResponse("pong"))
            }

            post<BuildsParams> { params ->
                val servers = call.receive<Servers>()
                validateRequestServers(servers)
                call.respond(fetchRunningBuilds(servers.servers, params))
            }
        }
    }
}

suspend fun respondBadRequest(call: ApplicationCall, message: String? = null) {
    call.respond(HttpStatusCode.BadRequest,
            ErrorResponse(HttpStatusCode.BadRequest.value,message ?: HttpStatusCode.BadRequest.description))
}