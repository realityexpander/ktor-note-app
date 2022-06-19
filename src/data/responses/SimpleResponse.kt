package com.realityexpander.data.responses

import io.ktor.http.*

data class SimpleResponse(
    val successful: Boolean,
    val statusCode: HttpStatusCode = HttpStatusCode.OK,
    val message: String
)
