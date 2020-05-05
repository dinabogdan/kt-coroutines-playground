package com.freesoft.playground

import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import org.http4k.server.Netty
import org.http4k.server.asServer

fun main() {

    val names = mapOf(
            "Jane" to "Dave",
            "Dave" to "Mary",
            "Mary" to "Pete",
            "Pete" to "Lucy",
            "Lucy" to "Waldo"
    )

    val lookupName = { request: Request ->
        val name = request.path("name")
        val headers = listOf("Content-Type" to "text/plain")
        val result = names[name]
        if (result != null) {
            Response(Status.OK)
                    .headers(headers)
                    .body(result)
        } else {
            Response(Status.NOT_FOUND)
                    .headers(headers)
                    .body("There is no match for $name")
        }
    }

    routes(
            "wheresWaldo" bind routes(
                    "/{name:.*}" bind Method.GET to lookupName
            )
    ).asServer(Netty(8080)).start()

}