package com.github.tadam.tcbuildreport.backend

data class ErrorResponse(val code: Int, val message: String = "")

data class BuildsResponse(val builds: List<ABuild>, val total: Int, val errors: List<String>? = null)

