package com.freesoft.playground

fun String.withThreadId() = "$this on thread ${Thread.currentThread().id}"