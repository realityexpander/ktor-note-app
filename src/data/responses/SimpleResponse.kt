package com.realityexpander.data.responses

import io.ktor.http.*

interface BaseSimpleResponse {
    val statusCode: HttpStatusCode
        get() = HttpStatusCode.OK
    val isSuccessful: Boolean
        get() = true
    val message: String
        get() = "OK"
}

data class SimpleResponse(
    override val isSuccessful: Boolean,
    override val statusCode: HttpStatusCode = HttpStatusCode.OK,
    override val message: String
) : BaseSimpleResponse

data class SimpleResponseWithData<T>(
    override val isSuccessful: Boolean,
    override val statusCode: HttpStatusCode = HttpStatusCode.OK,
    override val message: String,
    val data: T
) : BaseSimpleResponse

