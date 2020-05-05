package com.freesoft.playground

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import tornadofx.Controller
import tornadofx.Rest
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class HttpWaldoFinder : Controller() {
    private val api: Rest by inject()

    private suspend fun fetchNewName(inputName: String): String {
        val url = URI("http://localhost:8080/wheresWaldo/$inputName")
        val client = HttpClient.newBuilder().build()

        val handler = HttpResponse.BodyHandlers.ofString()
        val request = HttpRequest.newBuilder().uri(url).build()

        return withContext<String>(Dispatchers.IO) {
            println("Sending HTTP Request for $inputName".withThreadId())
            client.send(request, handler).body()
        }
    }

    private fun String.withThreadId() = "$this on thread ${Thread.currentThread().id}"


}