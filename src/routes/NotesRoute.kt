package com.realityexpander.routes

import com.realityexpander.data.*
import com.realityexpander.data.collections.Note
import com.realityexpander.data.requests.AddOwnerToNoteRequest
import com.realityexpander.data.requests.DeleteNoteRequest
import com.realityexpander.data.requests.NotesRequest
import com.realityexpander.data.responses.BaseSimpleResponse
import com.realityexpander.data.responses.SimpleResponse
import com.realityexpander.data.responses.SimpleResponseWithData
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
import kotlinx.coroutines.runBlocking
import kotlinx.html.unsafe

fun Route.notesRoute() {

    // Authenticated get request to get all notes for a user
    route("/getNotes") {
        authenticate {
            get {
                val email = call.principal<UserIdPrincipal>()!!.name
                val notes = getNotesForUserByEmail(email)
                call.respond(OK,
                    SimpleResponseWithData<List<Note>>(
                        successful = true, statusCode = HttpStatusCode.OK,
                        message = "${notes.size} note${addPostfixS(notes)} found",
                        data = notes
                    )
                )

                return@get
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
                val userExists = ifUserEmailExists(email)
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

                        return@post
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

                        return@post
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

                    return@post
                }
            }
        }
    }

    route("/deleteNote") {
        authenticate { // Authenticated post request to delete a note
            post {
                val email = call.principal<UserIdPrincipal>()!!.name

                val request = try {
                    call.receive<DeleteNoteRequest>()
                } catch (e: Exception) {
                    call.respond(
                        BadRequest,
                        SimpleResponse(
                            successful = false,
                            statusCode = HttpStatusCode.BadRequest,
                            message = "Invalid delete note format"
                        )
                    )
                    return@post
                }

                val userExists = ifUserEmailExists(email)
                if (userExists) {
                    val userId = getUserByEmail(email)!!.id

                    // Only delete the note for this userId
                    val acknowledged = deleteNoteIdForUserId(userId, request.id)

                    if (acknowledged) {
                        // Check if note still exists (ie: only an owner was removed)
                        val note = getNoteId(request.id)

                        if (note != null) {
                            call.respond(
                                OK,
                                SimpleResponseWithData(
                                    successful = true,
                                    statusCode = OK,
                                    message = "Owner removed from note",
                                    data = note
                                )
                            )

                            return@post
                        }

                        // Entire note was deleted
                        call.respond(
                            OK,
                            SimpleResponseWithData<Note?>(
                                successful = true,
                                statusCode = OK,
                                message = "Note deleted",
                                data = null
                            )
                        )
                        return@post
                    } else {
                        call.respond(
                            InternalServerError,
                            SimpleResponse(
                                successful = false,
                                statusCode = InternalServerError,
                                message = "Note not deleted (Owner not authorized)"
                            )
                        )

                        return@post
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

                    return@post
                }
            }
        }
    }

    route("/addOwnerToNote") {
        authenticate { // Authenticated post request to add an owner to a note
            post {
                val email = call.principal<UserIdPrincipal>()!!.name

                val request = try {
                    call.receive<AddOwnerToNoteRequest>()
                } catch (e: Exception) {
                    call.respond(
                        BadRequest,
                        SimpleResponse(
                            successful = false,
                            statusCode = BadRequest,
                            message = "Invalid 'add owner to note' format"
                        )
                    )

                    return@post
                }

                if (ifUserIdExists(request.ownerIdToAdd)) {

                    // Already an owner of this note?
                    if(isOwnerOfNoteId(request.ownerIdToAdd, request.noteId)) {
                        val note = getNoteId(request.noteId)!!

                        call.respond(
                            OK,
                            SimpleResponseWithData<Note?>(
                                successful = false,
                                statusCode = OK,
                                message = "${getEmailForUserId(request.ownerIdToAdd)} is already an owner of this note",
                                data = note
                            )
                        )

                        return@post
                    }

                    val acknowledged = addOwnerIdToNoteId(request.ownerIdToAdd, request.noteId)

                    if (acknowledged) {
                        val note = getNoteId(request.noteId)

                        if (note != null) {
                            call.respond(
                                OK,
                                SimpleResponseWithData(
                                    successful = true,
                                    statusCode = OK,
                                    message = "Owner added to note, " +
                                            "${getEmailForUserId(request.ownerIdToAdd)} can now see this note",
                                    data = note
                                )
                            )
                            return@post
                        }

                        // Problem finding updated note
                        call.respond(
                            InternalServerError,
                            SimpleResponseWithData<Note?>(
                                successful = false,
                                statusCode = InternalServerError,
                                message = "Note not updated - cant find note",
                                data = null
                            )
                        )

                        return@post

                    } else {
                        call.respond(
                            InternalServerError,
                            SimpleResponse(
                                successful = false,
                                statusCode = InternalServerError,
                                message = "Note not updated - Update failed"
                            )
                        )

                        return@post
                    }
                } else {
                    call.respond(
                        BadRequest,
                        SimpleResponse(
                            successful = false,
                            statusCode = BadRequest,
                            message = "User was not found - Can't add owner to note"
                        )
                    )

                    return@post
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
    val userExists = ifUserEmailExists(request.email)
    if (userExists) {
        val notes = getNotesForUserByEmail(request.email)

        if (notes.isNotEmpty()) {
            respondPlatform(
                isFromWeb,
                SimpleResponseWithData<List<Note>>(
                    successful = true, statusCode = HttpStatusCode.OK,
                    message = "${notes.size} note${addPostfixS(notes)} found",
                    data = notes
                )
            )

            return
        } else {
            respondPlatform(
                isFromWeb,
                SimpleResponseWithData<List<Note>>(
                    successful = true, statusCode = HttpStatusCode.OK,
                    message = "No Notes found for user ${request.email}",
                    data = emptyList()
                )
            )

            return
        }
    } else {
        respondPlatform(
            isFromWeb,
            SimpleResponse(
                false, HttpStatusCode.NotFound,
                "Error: Email does not exist"
            )
        )

        return
    }
}

private suspend fun ApplicationCall.respondPlatform(
    isFromWeb: Boolean,
    response: BaseSimpleResponse
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
    response: BaseSimpleResponse =
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
                                    runBlocking {
                                        renderNotesListHTML(response)
                                    }
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

private suspend fun ApplicationCall.renderNotesListHTML(response: SimpleResponseWithData<*>) =
    """
    <p>Raw response data:</p>
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
            <style>
                .italic {
                    font-style: italic; 
                    font-weight: 100;
                }
            </style>
            ${
                    it.map { note ->
                        """
                    <li style="background-color: ${note.color}; list-style: decimal; margin: 2px;">
                        <span class="italic">title: </span>${note.title}
                        <br>
                        <span class="italic">content: </span>${note.content}
                        <br>
                        <span class="italic">date: </span>${note.date}
                        <br>
                        <span class="italic">owner: </span>
                        <code>${note.owners.map {id -> 
                            getEmailForUserId(id) ?: "User not found"
                        }.joinToString(", ")}
                        </code>
                        <br>
                    </li>
                """
                    }.joinToString("")  // removes the []'s from the markup
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