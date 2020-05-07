package com.freesoft.playground

import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

data class EmailArgs(
        val sender: String,
        val receiver: String
)

data class EmailResult(
        val sender: String
)

//blocking api
fun sendEmail(emailArgs: EmailArgs): EmailResult {
    return EmailResult("a")
}

// non-blocking, callback style
fun sendEmail(emailArgs: EmailArgs, callback: (Throwable?, EmailResult?) -> Unit) {

}

// async await non blocking style

fun sendEmailAsync(emailArgs: EmailArgs): Future<EmailResult> {
    return CompletableFuture.completedFuture(EmailResult("a"))
}

// CompletableFuture + Coroutines
fun sendEmailAsyncKt(emailArgs: EmailArgs): Future<EmailResult> = future {
    ktSendEmail(emailArgs)
}

//coroutines style
suspend fun ktSendEmail(emailArgs: EmailArgs): EmailResult {
    return EmailResult("a")
}