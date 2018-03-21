package com.github.tadam.tcbuildreport.backend

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.gson.JsonSyntaxException
import io.ktor.application.Application
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.config.ApplicationConfig
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
import kotlinx.coroutines.experimental.newFixedThreadPoolContext
import org.redisson.Redisson
import org.redisson.codec.JsonJacksonCodec
import org.redisson.config.Config
import org.slf4j.LoggerFactory
import java.text.DateFormat

object BuildsFetcherHolder {
    var fetcher: BuildsFetcher? = null
}

object CacheHolder {
    var cache: Cache? = null
}

val log = LoggerFactory.getLogger("main")

fun Application.main() {
    setCache(environment.config)
    setBuildsFetcher(environment.config)

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
            setDateFormat(DateFormat.LONG)
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
                call.respond(BuildsFetcherHolder.fetcher!!.fetchBuilds(servers.servers, params))
            }
        }
    }
}

private suspend fun respondBadRequest(call: ApplicationCall, message: String? = null) {
    call.respond(HttpStatusCode.BadRequest,
            ErrorResponse(HttpStatusCode.BadRequest.value, message ?: HttpStatusCode.BadRequest.description))
}

private fun setBuildsFetcher(config: ApplicationConfig) {
    if (BuildsFetcherHolder.fetcher != null) return

    val restConfig = config.config("service.rest")

    val paramsNames = listOf("threadsNum", "fetchBuildsListTimeoutMs", "fetchBuildTimeoutMs", "maxServerConnections")
    val params = paramsNames.associate {
        val value = restConfig.property(it).getString().toInt()
        if (value <= 0) {
            throw RuntimeException("Wrong config value [$value] for parameter service.rest.$it")
        }
        Pair(it, value)
    }

    BuildsFetcherHolder.fetcher = BuildsFetcher(
            cache = CacheHolder.cache,
            ctx = newFixedThreadPoolContext(params.getValue("threadsNum"), BuildsFetcher.defaultContextName),
            fetchBuildsListTimeoutMs = params.getValue("fetchBuildsListTimeoutMs").toLong(),
            fetchBuildTimeoutMs = params.getValue("fetchBuildTimeoutMs").toLong(),
            maxServerConnections = params.getValue("maxServerConnections"))
}

private fun setCache(config: ApplicationConfig) {
    if (CacheHolder.cache != null) return

    try {
        val cacheConfig = config.config("service.cache")

        val redisNodes = cacheConfig.property("redisNodes").getString().split(',')
        val keyPrefix = cacheConfig.property("keyPrefix").getString()
        val expireABuildIdListS = cacheConfig.property("expireABuildIdListS ").getString().toLong()
        val expireABuildsS = cacheConfig.property("expireABuildsS").getString().toLong()

        val redissonConfig = Config()
        redissonConfig.useReplicatedServers().addNodeAddress(*redisNodes.toTypedArray())

        val objMapper = jacksonObjectMapper()
        val codec = JsonJacksonCodec(objMapper)
        redissonConfig.setCodec(codec)

        val redisson = Redisson.create(redissonConfig)

        CacheHolder.cache = Cache(redisson, keyPrefix, expireABuildIdListS, expireABuildsS)
    } catch (ex: Exception) {
        log.error("Cannot create Redis client: ${ex.message}")
    }
}