package com.github.tadam.tcbuildreport.backend

import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine
import org.redisson.api.RFuture

// analogous to the following code:
// https://github.com/Kotlin/kotlin-coroutines/blob/master/examples/future/await.kt
// just to be able to work with suspend functions rather than async style functions
//
// awaitCoro() name is used as await() is already taken
suspend fun <T> RFuture<T>.awaitCoro(): T =
        suspendCoroutine<T> { cont: Continuation<T> ->
            whenComplete { result, exception ->
                if (exception == null) // the future has been completed normally
                    cont.resume(result)
                else // the future has completed with an exception
                    cont.resumeWithException(exception)
            }
        }