package com.realityexpander.routes

import com.realityexpander.data.checkIfUserExists
import com.realityexpander.data.collections.Note
import com.realityexpander.data.getNotesForUser
import com.realityexpander.data.requests.NotesRequest
import com.realityexpander.data.responses.AppResponse
import com.realityexpander.data.responses.SimpleResponse
import com.realityexpander.data.responses.SimpleResponseWithData
import io.ktor.application.*
import io.ktor.html.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.pipeline.*
import kotlinx.html.unsafe

fun Route.notesRoute() {
    var isFromWeb = false

    get("/notes") {
        // val notes = getNotesForUser(call.parameters["email"] ?: "")
        isFromWeb = true

        val request = try {
            NotesRequest(email = call.parameters["email"]!!) // coming from the web
        } catch (e: ContentTransformationException) {
            call.respondPlatform(
                isFromWeb,
                SimpleResponse(
                    false, HttpStatusCode.BadRequest,
                    "Error: ${e.localizedMessage}"
                )
            )
            return@get
        } catch (e: Exception) {
            call.respondPlatform(
                isFromWeb,
                SimpleResponse(
                    false, HttpStatusCode.NotAcceptable,
                    "Error: ${e.localizedMessage}"
                )
            )
            return@get
        }

        getNotesRequest(request, isFromWeb)
    }

    post("/notes") {

        // Get the registration parameters
        val request = try {
            call.receive<NotesRequest>()  // coming from mobile app
        } catch (e: ContentTransformationException) {
            call.respondPlatform(
                isFromWeb,
                SimpleResponse(
                    false, HttpStatusCode.BadRequest,
                    "Error: ${e.localizedMessage}"
                )
            )
            return@post
        } catch (e: Exception) {
            call.respondPlatform(
                isFromWeb,
                SimpleResponse(
                    false, HttpStatusCode.NotAcceptable,
                    "Error: ${e.localizedMessage}"
                )
            )
            return@post
        }

        getNotesRequest(request, isFromWeb)
    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.getNotesRequest(
    request: NotesRequest,
    isFromWeb: Boolean
) {
    val userExists = checkIfUserExists(request.email)
    if (userExists) {
        val notes = getNotesForUser(request.email)

        if (notes.isNotEmpty()) {
            call.respondPlatform(
                isFromWeb,
                SimpleResponseWithData<List<Note>>(
                    successful = true, statusCode = HttpStatusCode.OK,
                    message = "${notes.size} notes found",
                    data = notes
                )
            )
        } else {
            call.respondPlatform(
                isFromWeb,
                SimpleResponseWithData<List<Note>>(
                    successful = true, statusCode = HttpStatusCode.OK,
                    message = "No Notes found for user ${request.email}",
                    data = emptyList()
                )
            )
        }
    } else {
        call.respondPlatform(
            isFromWeb,
            SimpleResponse(
                false, HttpStatusCode.NotFound,
                "Error: Email does not exist"
            )
        )
    }
}

private suspend fun ApplicationCall.respondPlatform(
    isFromWeb: Boolean,
    response: AppResponse
) {
    when (isFromWeb) {
        true -> {
            respondRawHTML(response)
            //call.respondRedirect("/")
//            respond(response.statusCode, response)
        }
        false -> {
            respond(response.statusCode, response)
        }
    }
}

private suspend fun ApplicationCall.respondRawHTML(
    response: AppResponse =
        SimpleResponse(
            successful = false,
            statusCode = HttpStatusCode.NotFound,
            message = "No Lists found"
        )
) {
    respondHtml {
        unsafe {
            raw(
                """
                    <!DOCTYPE html>
                    <html>
                      <head>
                        <title>Notes</title>
                        <style>
                            .status {
                                background-color: ${if (response.successful) "#008800" else "#880000"};
                                color: white;
                                padding: 10px;
                            }
                        </style>
                      </head>
                      
                      <body>
                        <h1>${if (response.successful) "Success" else "Error"}</h1>
                        <br>
                        <h2>
                            <div class="status">
                                <br>
                                <p>${response.message}</p>
                                ${if (!response.successful) "<br><p>Response code: ${response.statusCode}</p>" else ""}
                                <br>
                            </div>
                            <br>
                            ${
                                if (response is SimpleResponseWithData<*>) {
                                    renderNotes(response)
                                } else ""
                            }
                        </h2>
                        <br>
                      </body>
                    </html>
                """.trimIndent()
            )
        }
    }
}

private fun renderNotes(response: SimpleResponseWithData<*>) =
"""
    <p>Data:</p>
    <code>
        ${response.data.toString()}
    </code>
    <br>
    ${
        @Suppress("UNCHECKED_CAST") // we know it's a List<Note>
        (response.data as? List<Note>)?.let {
        """
            <p>${it.size} note${if (it.size > 1) "s" else ""} found</p>
            <ul>
            ${
                it.map { note ->
                """
                    <li style="background-color: ${note.color}; list-style: decimal;">
                        title: ${note.title}
                        <br>
                        content: ${note.content}
                        <br>
                        date: ${note.date}
                    </li>
                """
                }.joinToString("")  // removes the [] from the markup
            }
            </ul>
        """.trimIndent()
        } ?: ""
    }                                   
""".trimIndent()