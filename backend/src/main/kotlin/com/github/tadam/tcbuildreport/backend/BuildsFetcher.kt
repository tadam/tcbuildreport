package com.github.tadam.tcbuildreport.backend

import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.*
import org.jetbrains.teamcity.rest.BuildId
import org.jetbrains.teamcity.rest.TeamCityInstance
import org.jetbrains.teamcity.rest.TeamCityInstanceFactory
import java.io.IOException
import kotlin.coroutines.experimental.CoroutineContext

data class BuildsAndErrors(val builds: List<ABuild>, val errors: List<String>)
data class BuildOrError(val build: ABuild? = null, val error: String? = null)

class BuildsFetcher(val cache: Cache? = null,
                    val ctx: CoroutineContext = newFixedThreadPoolContext(1, defaultContextName),
                    val fetchBuildsListTimeoutMs: Long = 10000L,
                    val fetchBuildTimeoutMs: Long = 2000L,
                    val maxServerConnections: Int = 10)
{
    companion object {
        const val defaultContextName = "tcRestPool"
    }

    fun fetchBuilds(servers: List<Server>, params: BuildsParams): BuildsResponse = runBlocking {
        val builds = mutableListOf<ABuild>()
        val errors = mutableListOf<String>()

        val serverResultsDeferred = servers.map {
            async(ctx) {
                fetchBuildsFromServer(it)
            }
        }

        for (deferred in serverResultsDeferred) {
            val res = deferred.await()
            builds.addAll(res.builds)
            errors.addAll(res.errors)
        }


        val cmpProperty = compareBy<ABuild>({
            when (params.sortBy) {
                SortBy.server -> it.server
                SortBy.startDate -> it.startDate
            }
        })

        val cmpPropertyOrder = when (params.sortOrder) {
            SortOrder.asc -> cmpProperty
            SortOrder.desc -> cmpProperty.reversed()
        }

        builds.sortWith(cmpPropertyOrder)


        var buildsLimited = mutableListOf<ABuild>()
        if (params.offset < builds.size) {
            val requestedToIndex = params.offset + params.limit
            val toIndex = if (requestedToIndex < builds.size) requestedToIndex else builds.size
            buildsLimited = builds.subList(params.offset, toIndex)
        }

        BuildsResponse(buildsLimited, builds.size, errors)
    }

    private suspend fun fetchBuildsFromServer(server: Server): BuildsAndErrors {
        val service = if (server.credentials == null)
            TeamCityInstanceFactory.guestAuth(server.url) else
            TeamCityInstanceFactory.httpAuth(server.url, server.credentials.login, server.credentials.password)

        val resBuilds = mutableListOf<ABuild>()
        val errors = mutableListOf<String>()

        var buildIds: ABuildIdList?
        buildIds = cache?.getABuildIdList(server.url)

        if (buildIds == null) {
            buildIds = try {
                withTimeout(fetchBuildsListTimeoutMs) {
                    service.builds().withAnyStatus().runningOnly().list().map{ it.id.stringId }
                }
            } catch (ex: CancellationException) {
                errors.add("Couldn't retrieve list of builds from [${server.url}]: timed out")
                return BuildsAndErrors(resBuilds, errors)
            } catch (ex: IOException) {
                errors.add("Couldn't retrieve list of builds from [${server.url}]: [${ex.message}]")
                return BuildsAndErrors(resBuilds, errors)
            }

            cache?.setABuildIdList(server.url, buildIds)
        }

        var buildIdsToFetch = buildIds.toSet()
        var buildIdsToDelFromCache = setOf<ABuildId>()
        if (cache != null) {
            val cachedBuildIds = cache.getABuildsKeySet(server.url)
            if (cachedBuildIds != null) {
                buildIdsToDelFromCache = cachedBuildIds.minus(buildIds)

                val cachedBuilds = cache.getABuilds(server.url, buildIds)
                buildIdsToFetch = buildIdsToFetch.minus(cachedBuilds.map { it.key })
                resBuilds.addAll(cachedBuilds.values)
            }
        }

        val buildsAndErrorsFetched = fetchFullBuildsBatch(service, server.url, buildIdsToFetch)
        resBuilds.addAll(buildsAndErrorsFetched.builds)
        errors.addAll(buildsAndErrorsFetched.errors)

        cache?.setAndDeleteABuilds(server.url, buildsAndErrorsFetched.builds, buildIdsToDelFromCache)

        return BuildsAndErrors(resBuilds, errors)
    }

    private suspend fun fetchFullBuildsBatch(
            service: TeamCityInstance, serverUrl: ServerUrl, buildIds: Collection<ABuildId>): BuildsAndErrors
    {
        val channel = ArrayChannel<Deferred<BuildOrError>>(maxServerConnections)

        val buildsFetched = mutableListOf<ABuild>()
        val errors = mutableListOf<String>()

        val buildReceiveJob = launch(ctx) {
            while (!channel.isClosedForReceive) {
                val (build, error) = channel.receive().await()
                if (build != null) buildsFetched.add(build)
                if (error != null) errors.add(error)
            }
        }

        buildIds.forEach { aBuildId ->
            channel.sendBlocking(
                    async(ctx) {
                        fetchFullBuild(service, serverUrl, BuildId(aBuildId))
                    }
            )
        }

        channel.close()
        buildReceiveJob.join()

        return BuildsAndErrors(buildsFetched, errors)
    }

    private suspend fun fetchFullBuild(service: TeamCityInstance, serverUrl: ServerUrl, id: BuildId): BuildOrError {
        try {
            return withTimeout(fetchBuildTimeoutMs) {
                // service.build() gets full build info asynchronously and subsequent call fetchStartDate()
                // doesn't actually fetch anything;
                // if partialBuild.fetchStartDate() is called, then it's going to be blocking
                val fullBuild = service.build(id)

                val build = ABuild(server = serverUrl,
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
}
