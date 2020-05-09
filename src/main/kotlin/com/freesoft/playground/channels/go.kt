package com.freesoft.playground.channels

import com.freesoft.playground.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.locks.ReentrantLock
import kotlin.coroutines.*

open class Pool(
        private val pool: ForkJoinPool
) : AbstractCoroutineContextElement(ContinuationInterceptor),
        ContinuationInterceptor {
    override fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T> =
            PoolContinuation(pool, continuation.context.fold(continuation) { _continuation, element ->
                if (element != this@Pool && element is ContinuationInterceptor) {
                    element.interceptContinuation(_continuation)
                } else _continuation
            })

    fun runParallel(block: suspend () -> Unit) {
        pool.execute { launch(this, block) }
    }
}

private class PoolContinuation<T>(
        val pool: ForkJoinPool,
        val cont: Continuation<T>
) : Continuation<T> {
    override val context: CoroutineContext = cont.context

    override fun resumeWith(result: Result<T>) {
        pool.execute { cont.resumeWith(result) }
    }
}

object CommonPool : Pool(ForkJoinPool.commonPool())

fun <T> runBlocking(context: CoroutineContext, block: suspend () -> T): T =
        BlockingCoroutine<T>(context).also { block.startCoroutine(it) }.getValue()

private class BlockingCoroutine<T>(override val context: CoroutineContext) : Continuation<T> {
    private val lock = ReentrantLock()
    private val done = lock.newCondition()
    private var result: Result<T>? = null

    private inline fun <T> locked(block: () -> T): T {
        lock.lock()
        return try {
            block()
        } finally {
            lock.unlock()
        }
    }

    private inline fun loop(block: () -> Unit): Nothing {
        while (true) {
            block()
        }
    }

    override fun resumeWith(result: Result<T>) = locked {
        this.result = result
        done.signal()
    }

    fun getValue(): T = locked<T> {
        loop {
            val result = this.result
            if (result == null) {
                done.awaitUninterruptibly()
            } else {
                return@locked result.getOrThrow()
            }
        }
    }
}

fun mainBlocking(block: suspend () -> Unit) = runBlocking(CommonPool, block)

fun go(block: suspend () -> Unit) = CommonPool.runParallel(block)