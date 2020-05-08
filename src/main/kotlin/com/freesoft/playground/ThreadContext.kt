package com.freesoft.playground

import kotlinx.coroutines.delay
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.Continuation
import kotlin.coroutines.ContinuationInterceptor
import kotlin.coroutines.CoroutineContext
import kotlin.system.measureTimeMillis

// Cooperative multi tasking; this code is done in order to provide thread confinedness


fun newFixedThreadPoolContext(nThreads: Int, name: String) = ThreadContext(nThreads, name)
fun newSingleThreadContext(name: String) = ThreadContext(1, name)

class ThreadContext(
        nThreads: Int,
        private val name: String
) : AbstractCoroutineContextElement(ContinuationInterceptor),
        ContinuationInterceptor {

    private val threadNo = AtomicInteger()

    val executor: ScheduledExecutorService = Executors.newScheduledThreadPool(nThreads) { target ->
        thread(start = false, isDaemon = true, name = name + "-" + threadNo.incrementAndGet()) {
            target.run()
        }
    }

    private inner class ThreadContinuation<T>(
            val cont: Continuation<T>
    ) : Continuation<T> {
        override val context: CoroutineContext = cont.context

        override fun resumeWith(result: Result<T>) {
            executor.execute { cont.resumeWith(result) }
        }
    }

    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> =
            ThreadContinuation(continuation.context.fold(continuation) { _continuation, element ->
                if (element != this@ThreadContext && element is ContinuationInterceptor) {
                    element.interceptContinuation(_continuation)
                } else _continuation
            })

}

fun main() {
    val timer = measureTimeMillis {
        println("Starting MyEventThread")
        val context = newSingleThreadContext("MyEventThread")
        val f = future(context) {
            println("Hello world!")

            val future1 = future(context) {
                println("future1 is sleeping")
//                delay(1000)
                Thread.sleep(1000)
                println("future1 returns 1")
                1
            }

            val future2 = future(context) {
                println("future2 is sleeping")
//                delay(1000)
                Thread.sleep(1000)
                println("future2 returns 2")
                2
            }

            println("wait for both future1 and future2")
            val sum = future1.await() + future2.await()
            println("the sum is: $sum")
        }
        f.get()
        println("Terminated")
    }
    println("Terminated in: $timer")
}