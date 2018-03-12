package com.github.tadam.tcbuildreport.backend

import io.ktor.application.*
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import kotlin.test.*


class ApiTest {
    @Test
    fun pingRequest() = withTestApplication(Application::api) {
        with(handleRequest(HttpMethod.Get, "/api/ping") {
            addHeader(HttpHeaders.Accept, "application/json") })
        {
            println("CONTENT: " + response.content)
            assertEquals(HttpStatusCode.OK, response.status())
            //assertEquals("Test String", response.content)
        }
    }
}
