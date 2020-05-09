package com.freesoft.playground.channels

import com.freesoft.playground.delay
import com.freesoft.playground.withThreadId

suspend fun say(s: String) {
    for (i in 0..4) {
        delay(1000)
        println(s)
    }
}

fun main() = mainBlocking {
    go { say("World".withThreadId()) }
    say("hello".withThreadId())
}