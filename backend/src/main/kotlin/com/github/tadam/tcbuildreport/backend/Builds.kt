package com.github.tadam.tcbuildreport.backend

fun fetchRunningBuilds(servers: List<Server>, params: BuildsParams): BuildsResponse {
    val builds = mutableListOf<Build>()
    val total = 0
    val errors = null
    return BuildsResponse(builds, total, errors)
}