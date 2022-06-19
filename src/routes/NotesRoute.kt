package com.realityexpander.routes

import com.realityexpander.data.checkIfUserExists
import com.realityexpander.data.collections.Note
import com.realityexpander.data.getNotesForUser
import com.realityexpander.data.requests.NotesRequest
import com.realityexpander.data.responses.AppResponse
import com.realityexpander.data.responses.SimpleResponse
import com.realityexpander.data.responses.SimpleResponseWithData
import com.realityexpander.data.saveNote
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.html.*
import io.ktor.http.*
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.html.unsafe

fun Route.notesRoute() {

    // Authenticated get request to get all notes for a user
    route("/getNotes") {
        authenticate {
            get {
                val email = call.principal<UserIdPrincipal>()!!.name
                val notes = getNotesForUser(email)
                call.respond(OK,
                    SimpleResponseWithData<List<Note>>(
                        successful = true, statusCode = HttpStatusCode.OK,
                        message = "${notes.size} note${addPostfixS(notes)} found",
                        data = notes
                    )
                )
            }
        }
    }

    route("/saveNote") {
        authenticate {
            post {
                val note = try {
                    call.receive<Note>()
                } catch (e: Exception) {
                    call.respond(
                        OK,
                        SimpleResponse(
                            successful = false,
                            statusCode = HttpStatusCode.BadRequest,
                            message = "Invalid note format"
                        )
                    )
                    return@post
                }

                val email = call.principal<UserIdPrincipal>()!!.name
                val userExists = checkIfUserExists(email)
                if (userExists) {
                    val acknowledged = saveNote(note)

                    if (acknowledged) {
                        call.respond(
                            OK,
                            SimpleResponseWithData(
                                successful = true,
                                statusCode = OK,
                                message = "Note added",
                                data = note
                            )
                        )
                    } else {
                        call.respond(
                            OK,
                            SimpleResponseWithData(
                                successful = false,
                                statusCode = InternalServerError,
                                message = "Note not added",
                                data = note
                            )
                        )
                    }
                } else {
                    call.respond(
                        BadRequest,
                        SimpleResponse(
                            successful = false,
                            statusCode = BadRequest,
                            message = "User not found"
                        )
                    )
                }
            }
        }
    }

    // Insecure get request to get all notes for a user (setup for testing)
    get("/notes") {
        var isFromWeb = false

        val request = try {
            if (call.request.queryParameters["email"] != null) {
                isFromWeb = true
                NotesRequest(email = call.parameters["email"]!!) // coming from the web (query params)
            } else {
                isFromWeb = false
                call.receive<NotesRequest>()  // coming from mobile app (body json)
            }
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

        call.getNotesRequest(request, isFromWeb)
    }
}

private suspend fun ApplicationCall.getNotesRequest(
    request: NotesRequest,
    isFromWeb: Boolean
) {
    val userExists = checkIfUserExists(request.email)
    if (userExists) {
        val notes = getNotesForUser(request.email)

        if (notes.isNotEmpty()) {
            respondPlatform(
                isFromWeb,
                SimpleResponseWithData<List<Note>>(
                    successful = true, statusCode = HttpStatusCode.OK,
                    message = "${notes.size} note${addPostfixS(notes)} found",
                    data = notes
                )
            )
        } else {
            respondPlatform(
                isFromWeb,
                SimpleResponseWithData<List<Note>>(
                    successful = true, statusCode = HttpStatusCode.OK,
                    message = "No Notes found for user ${request.email}",
                    data = emptyList()
                )
            )
        }
    } else {
        respondPlatform(
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
                                <p>Message from server: ${response.message}</p>
                                ${if (!response.successful) "<br><p>Response code: ${response.statusCode}</p>" else ""}
                                <br>
                            </div>
                            <br>
                            ${
                                if (response is SimpleResponseWithData<*>) {
                                    renderNotesList(response)
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

private fun renderNotesList(response: SimpleResponseWithData<*>) =
"""
    <p>Data:</p>
    <code>
        ${response.data.toString()}
    </code>
    <br>
    ${
        try {
            @Suppress("UNCHECKED_CAST") // we're pretty sure it's a `List<Note>`
            (response.data as? List<Note>)?.let {
                """
            <p>${it.size} note${addPostfixS(it)} found</p>
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
        } catch (e: Exception) {  // just in case the cast fails
            e.printStackTrace()
            "<br><p style=\"color:red; background-color:black;\">" +
                    "an error occurred ${e.localizedMessage}</p><br>"
        }
    }                                   
""".trimIndent()

// Add s to the end of the string if it's not 1
private fun addPostfixS(it: List<Note>) =
    if (it.size > 1) "s" else ""