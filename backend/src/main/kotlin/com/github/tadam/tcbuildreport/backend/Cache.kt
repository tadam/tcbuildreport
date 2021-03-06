package com.github.tadam.tcbuildreport.backend

import org.redisson.api.RedissonClient
import org.slf4j.LoggerFactory
import java.util.Base64
import java.util.concurrent.TimeUnit

class Cache(private val client: RedissonClient,
            private val keyPrefix: String = "",
            private val expireABuildIdListS: Long = 120L,
            private val expireABuildsS: Long = 3600L)
{
    companion object {
        const val aBuildIdListKeyInfix = "ABuildIdList"
        const val aBuildsKeyInfix = "ABuilds"

        private val log = LoggerFactory.getLogger(::Cache.javaClass)
    }

    suspend fun getABuildIdList(serverUrl: ServerUrl): ABuildIdList? {
        return try {
            client.getBucket<ABuildIdList>(encodeABuildIdListKey(serverUrl)).getAsync().awaitCoro()
        } catch (ex: Exception) {
            log.error(ex.message)
            null
        }
    }

    suspend fun setABuildIdList(serverUrl: ServerUrl, buildIds: ABuildIdList) {
        try {
            val rBucket = client.getBucket<ABuildIdList>(encodeABuildIdListKey(serverUrl))
            rBucket.setAsync(buildIds).awaitCoro()
            rBucket.expireAsync(expireABuildIdListS, TimeUnit.SECONDS).awaitCoro()
        } catch (ex: Exception) {
            log.error(ex.message)
        }
    }

    suspend fun getABuildsKeySet(serverUrl: ServerUrl): Set<ABuildId>? {
        return try {
            client.getMap<ABuildId, ABuild>(encodeABuildsKey(serverUrl)).readAllKeySetAsync().awaitCoro()
        } catch (ex: Exception) {
            log.error(ex.message)
            null
        }
    }

    suspend fun getABuilds(serverUrl: ServerUrl, buildIds: ABuildIdList): Map<ABuildId, ABuild> {
        try {
            val rMap = client.getMap<ABuildId, ABuild>(encodeABuildsKey(serverUrl))
            return rMap.getAllAsync(buildIds.toSet()).awaitCoro()
        } catch (ex: Exception) {
            log.error(ex.message)
            return mapOf<ABuildId, ABuild>()
        }
    }

    suspend fun setAndDeleteABuilds(serverUrl: ServerUrl, buildsToSet: List<ABuild>, buildIdsToDel: Collection<ABuildId>) {
        try {
            val rMap = client.getMap<ABuildId, ABuild>(encodeABuildsKey(serverUrl))

            val futureRemove = rMap.fastRemoveAsync(*buildIdsToDel.toTypedArray())
            val futurePut = rMap.putAllAsync(buildsToSet.associate { it.id to it })
            futureRemove.awaitCoro()
            futurePut.awaitCoro()

            rMap.expireAsync(expireABuildsS, TimeUnit.SECONDS).awaitCoro()
        } catch (ex: Exception) {
            log.error(ex.message)
        }
    }

    private fun encodeABuildIdListKey(serverUrl: ServerUrl) =  encodeKey(aBuildIdListKeyInfix, serverUrl)

    private fun encodeABuildsKey(serverUrl: String) = encodeKey(aBuildsKeyInfix, serverUrl)

    private fun encodeKey(infix: String, serverUrl: ServerUrl): String {
        val serverUrlBase64 = Base64.getEncoder().encodeToString(serverUrl.toByteArray())
        return "${keyPrefix}:${infix}:${serverUrlBase64}"
    }
}
