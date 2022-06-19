package com.realityexpander.data.responses

import io.ktor.http.*
import io.ktor.http.cio.*

data class SimpleResponse(
    override val successful: Boolean,
    override val statusCode: HttpStatusCode = HttpStatusCode.OK,
    override val message: String
) : AppResponse

data class SimpleResponseWithData<T>(
    override val successful: Boolean,
    override val statusCode: HttpStatusCode = HttpStatusCode.OK,
    override val message: String,
    val data: T
) : AppResponse

interface AppResponse {
    val statusCode: HttpStatusCode
        get() = HttpStatusCode.OK
    val successful: Boolean
        get() = true
    val message: String
        get() = "OK"
}