package com.freesoft.playground.channels

import com.freesoft.playground.delay

suspend fun fibonacci(n: Int, c: SendChannel<Int>) {
    var x = 0
    var y = 1
    for (i in 0 until n) {
        delay(1000)
        c.send(x)
        val next = x + y
        x = y
        y = next
    }
    c.close()
}

fun main() = mainBlocking {
    val c = Channel<Int>(2)
    go { fibonacci(10, c) }
    for (i in c) {
        println(i)
    }
}