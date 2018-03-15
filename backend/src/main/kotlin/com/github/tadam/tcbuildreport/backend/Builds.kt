package com.github.tadam.tcbuildreport.backend

import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.*
import org.jetbrains.teamcity.rest.BuildId
import org.jetbrains.teamcity.rest.TeamCityInstance
import org.jetbrains.teamcity.rest.TeamCityInstanceFactory
import java.io.IOException

data class BuildsAndErrors(val builds: List<Build>, val errors: List<String>? = null)
data class BuildOrError(val build: Build? = null, val error: String? = null)

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
        errors.add("Couldn't retrieve list of builds from [${server.url}]: timed out")
        return BuildsAndErrors(resBuilds, errors)
    } catch (ex: IOException) {
        errors.add("Couldn't retrieve list of builds from [${server.url}]: [${ex.message}]")
        return BuildsAndErrors(resBuilds, errors)
    }


    val channel = ArrayChannel<Deferred<BuildOrError>>(10)

    launch {
        while (!channel.isClosedForReceive) {
            val (build, error) = channel.receive().await()
            if (build != null) resBuilds.add(build)
            if (error != null) errors.add(error)
        }
    }

    builds.forEach { partialBuild ->
        channel.sendBlocking(
                async {
                    fetchFullBuild(service, server.url, partialBuild.id)
                }
        )
    }

    channel.close()

    return BuildsAndErrors(resBuilds, errors)
}

private suspend fun fetchFullBuild(service: TeamCityInstance, serverUrl: String, id: BuildId): BuildOrError {
    try {
        return withTimeout(2000L) {
            // service.build() gets full build info asynchronously and subsequent call fetchStartDate()
            // doesn't actually fetch anything;
            // if partialBuild.fetchStartDate() is called, then it's going to be blocking
            val fullBuild = service.build(id)

            val build = Build(server = serverUrl,
                    id = fullBuild.id.stringId,
                    buildTypeId = fullBuild.buildTypeId.stringId,
                    buildNumber = fullBuild.buildNumber,
                    startDate = fullBuild.fetchStartDate(),
                    webUrl = fullBuild.getWebUrl())

            BuildOrError(build)
        }
    } catch (ex: CancellationException) {
        return BuildOrError(error = "Couldn't retrieve build with id ${id.stringId} from [$serverUrl]: timed out")
    } catch (ex: IOException) {
        return BuildOrError(error = "Couldn't retrieve build with id ${id.stringId} from [$serverUrl]: [${ex.message}]")
    }
}