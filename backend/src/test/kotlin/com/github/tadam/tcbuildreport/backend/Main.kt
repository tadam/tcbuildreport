package com.github.tadam.tcbuildreport.backend

import io.ktor.application.Application
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import kotlin.test.Test
import kotlin.test.assertEquals


class ApiTest {
    @Test
    fun pingRequest() = withTestApplication(Application::main) {
        handleRequest(HttpMethod.Get, "/api/ping").response.let {
            assertEquals(HttpStatusCode.OK, it.status())
        }
    }
}
