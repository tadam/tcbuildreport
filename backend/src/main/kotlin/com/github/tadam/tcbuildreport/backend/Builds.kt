package com.github.tadam.tcbuildreport.backend

import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.*
import org.jetbrains.teamcity.rest.BuildId
import org.jetbrains.teamcity.rest.TeamCityInstance
import org.jetbrains.teamcity.rest.TeamCityInstanceFactory

data class BuildsAndErrors(val builds: List<Build>, val errors: List<String>? = null)

fun fetchBuilds(servers: List<Server>, params: BuildsParams): BuildsResponse = runBlocking {
    val builds = mutableListOf<Build>()
    val errors = mutableListOf<String>()

    val serverResultsDeferred = servers.map {
        async {
            fetchBuildsFromServer(it)
        }
    }

    for (deferred in serverResultsDeferred) {
        val res = deferred.await()
        builds.addAll(res.builds)
        if (res.errors != null) errors.addAll(res.errors)
    }

    BuildsResponse(builds, builds.size, errors)
}

private suspend fun fetchBuildsFromServer(server: Server): BuildsAndErrors {
    val service = if (server.credentials == null)
        TeamCityInstanceFactory.guestAuth(server.url) else
        TeamCityInstanceFactory.httpAuth(server.url, server.credentials.login, server.credentials.password)

    val resBuilds = mutableListOf<Build>()
    val errors = mutableListOf<String>()

    val builds = try {
         withTimeout(10000L) {
             service.builds().withAnyStatus().runningOnly().list()
         }
    } catch (ex: CancellationException) {
        errors.add("Retrieving list of running builds for server [${server.url}] is timed out")
        return BuildsAndErrors(resBuilds, errors)
    }


    val channel = ArrayChannel<Deferred<Pair<Build?, String>>>(10)

    launch {
        while (!channel.isClosedForReceive) {
            val (buildOrNull, id) = channel.receive().await()
            if (buildOrNull != null) {
                resBuilds.add(buildOrNull)
            } else {
                errors.add("Retrieving build with id $id on server [${server.url}] is timed out")
            }
        }
    }

    builds.forEach { partialBuild ->
        channel.sendBlocking(
                async {
                    val buildOrNull = fetchFullBuild(service, server.url, partialBuild.id)
                    Pair(buildOrNull, partialBuild.id.stringId)
                }
        )
    }

    channel.close()

    return BuildsAndErrors(resBuilds, errors)
}

private suspend fun fetchFullBuild(service: TeamCityInstance, serverUrl: String, id: BuildId): Build? {
    try {
        return withTimeout(2000L) {
            // service.build() gets full build info asynchronously and subsequent call fetchStartDate()
            // doesn't actually fetch anything;
            // if partialBuild.fetchStartDate() is called, then it's going to be blocking
            val fullBuild = service.build(id)

            Build(server = serverUrl,
                    id = fullBuild.id.stringId,
                    buildTypeId = fullBuild.buildTypeId.stringId,
                    buildNumber = fullBuild.buildNumber,
                    startDate = fullBuild.fetchStartDate(),
                    webUrl = fullBuild.getWebUrl())
        }
    } catch (ex: CancellationException) {
        return null
    }
}