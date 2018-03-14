package com.github.tadam.tcbuildreport.backend

import java.util.Date


data class PingResponse(val data: String)

data class ErrorResponse(val code: Int, val message: String = "")

data class BuildsResponse(val builds: List<Build>, val total: Int, val errors: List<String>? = null)

data class Build(
        val server: String,
        val id: String,
        val buildTypeId: String,
        val buildNumber: String,
        val startDate: Date,
        val webUrl: String)
