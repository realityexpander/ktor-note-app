package com.realityexpander.routes

import com.realityexpander.data.*
import com.realityexpander.data.collections.Note
import com.realityexpander.data.requests.AddOwnerIdToNoteIdRequest
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
import io.ktor.http.HttpStatusCode.Companion.NotAcceptable
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.coroutines.runBlocking
import kotlinx.html.*
import kotlinx.html.unsafe

fun Route.notesRoute() {

    // Authenticated get request to get all notes for a user
    route("/getNotes") {
        authenticate {
            get {
                // get the email from the authenticated user object
                val email = call.principal<UserIdPrincipal>()!!.name
                val notes = getNotesForUserByEmail(email)

                call.respond(OK,
                    SimpleResponseWithData<List<Note>>(
                        isSuccessful = true, statusCode = OK,
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
                            isSuccessful = false,
                            statusCode = BadRequest,
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
                                isSuccessful = true,
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
                                isSuccessful = false,
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
                            isSuccessful = false,
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
                            isSuccessful = false,
                            statusCode = BadRequest,
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
                                    isSuccessful = true,
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
                                isSuccessful = true,
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
                                isSuccessful = false,
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
                            isSuccessful = false,
                            statusCode = BadRequest,
                            message = "User not found"
                        )
                    )

                    return@post
                }
            }
        }
    }

    route("/addOwnerIdToNoteId") {
        authenticate { // Authenticated post request to add an owner to a note
            post {
                val email = call.principal<UserIdPrincipal>()!!.name

                val request = try {
                    call.receive<AddOwnerIdToNoteIdRequest>()
                } catch (e: Exception) {
                    call.respond(
                        BadRequest,
                        SimpleResponse(
                            isSuccessful = false,
                            statusCode = BadRequest,
                            message = "Invalid 'add owner to note' format"
                        )
                    )

                    return@post
                }

                // Show incoming request headers
                //println("headers: ${call.request.headers.entries()}")

                if (ifUserIdExists(request.ownerIdToAdd)) {

                    // Already an owner of this note?
                    if(isOwnerOfNoteId(request.ownerIdToAdd, request.noteId)) {
                        val note = getNoteId(request.noteId)!!

                        call.respond(
                            OK,
                            SimpleResponseWithData<Note?>(
                                isSuccessful = false,
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
                                    isSuccessful = true,
                                    statusCode = OK,
                                    message = "Owner added to note, " +
                                            "${getEmailForUserId(request.ownerIdToAdd)} can now access this note",
                                    data = note
                                )
                            )
                            return@post
                        }

                        // Problem finding updated note
                        call.respond(
                            InternalServerError,
                            SimpleResponseWithData<Note?>(
                                isSuccessful = false,
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
                                isSuccessful = false,
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
                            isSuccessful = false,
                            statusCode = BadRequest,
                            message = "User was not found - Can't add owner to note"
                        )
                    )

                    return@post
                }
            }
        }
    }

    get("/getOwnerIdForEmail") {
        val email = call.parameters["email"]!!
        val user = getUserByEmail(email)
        if (user != null) {
            call.respond(
                OK,
                SimpleResponseWithData(
                    isSuccessful = true,
                    statusCode = OK,
                    message = "User found",
                    data = user.id
                )
            )
        } else {
            call.respond(
                BadRequest,
                SimpleResponse(
                    isSuccessful = false,
                    statusCode = BadRequest,
                    message = "User not found"
                )
            )
        }
    }

    get("/getEmailForOwnerId") {
        val ownerId = call.parameters["ownerId"]!!
        val email = getEmailForUserId(ownerId)
        if (email != null) {
            call.respond(
                OK,
                SimpleResponseWithData(
                    isSuccessful = true,
                    statusCode = OK,
                    message = "User found",
                    data = email
                )
            )
        } else {
            call.respond(
                BadRequest,
                SimpleResponse(
                    isSuccessful = false,
                    statusCode = BadRequest,
                    message = "User not found"
                )
            )
        }
    }

    // Unauthenticated request to get all notes for one user with the email as html (setup for debugging)
    get("/getAllNotesForEmail") {
        var isFromWeb = false

        val request = try {
            if (call.request.queryParameters["email"] != null) {
                // coming from the web (query params)
                isFromWeb = true
                NotesRequest(email = call.parameters["email"]!!)
            } else {
                // coming from mobile app (body json)
                isFromWeb = false
                call.receive<NotesRequest>()
            }
        } catch (e: ContentTransformationException) {
            call.respondPlatform(
                isFromWeb,
                SimpleResponse(
                    false, BadRequest,
                    "Error: ${e.localizedMessage}"
                )
            )

            return@get
        } catch (e: Exception) {
            call.respondPlatform(
                isFromWeb,
                SimpleResponse(
                    false, NotAcceptable,
                    "Error: ${e.localizedMessage}"
                )
            )

            return@get
        }

        call.getNotesRequest(request, isFromWeb)
    }

    route("/getAllNotesDsl") {
        //authenticate {
            get {

                val email = if (call.request.queryParameters["email"] != null) {
                     call.parameters["email"]!!
                } else {
                    ""
                }
                val allNotes = getNotesForUserByEmail(email)

                call.respondHtml {
                    head {
                        styleLink("/static/css/styles.css")  // from StyleRoute.kt
                    }
                    body {
                        h1 {
                            +"All Notes"
                        }
                        if(email.isBlank()) {
                            +"No email provided"
                        } else {
                            +"Email: $email"
                        }
                        for(note in allNotes) {
                            h3 {
                                +"${note.title} (Belongs to: ${
                                    note.owners.map { ownerId ->
                                        runBlocking {
                                            getEmailForUserId(ownerId)
                                        }
                                    }.joinToString(", ")
                                })"

                            }
                            p {
                                +note.content
                            }
                            br
                        }
                        div {
                            h2 {

                                +"Showing rule(\"div h2\") here"
                            }
                        }
                    }
                }
            }
        //}
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
                    isSuccessful = true, statusCode = OK,
                    message = "${notes.size} note${addPostfixS(notes)} found",
                    data = notes
                )
            )

            return
        } else {
            respondPlatform(
                isFromWeb,
                SimpleResponseWithData<List<Note>>(
                    isSuccessful = true, statusCode = OK,
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
            isSuccessful = false,
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
                            body, html {
                                background-color: #222222;
                                color: #BBBBBB;
                            }
                            .status {
                                background-color: ${if (response.isSuccessful) "#008800" else "#880000"};
                                color: white;
                                padding: 10px;
                            }
                        </style>
                      </head>
                      
                      <body>
                        <h1>${if (response.isSuccessful) "Success" else "Error"}</h1>
                        <br>
                        <h2>
                            <div class="status">
                                <br>
                                <p>Message from server: ${response.message}</p>
                                ${if (!response.isSuccessful) "<br><p>Response code: ${response.statusCode}</p>" else ""}
                                <br>
                            </div>
                            <br>
                            ${
                                // If it's a success, there will be data attached.
                                if (response is SimpleResponseWithData<*>) {
                                    runBlocking {
                                        @Suppress("UNCHECKED_CAST")
                                        renderNotesListHTML(response as SimpleResponseWithData<List<Note>>)
                                    }
                                } else "No Data"
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

private suspend fun ApplicationCall.renderNotesListHTML(response: SimpleResponseWithData<List<Note>>) =
    """
    <p>Raw response data:</p>
    <code>
    ${
        // Show the raw data
        response.data.let { notes ->
            notes.joinToString(
                prefix = "• [",
                separator = "]<br>• [",
                postfix = "]"
            ) { it.toString() }
        }
    }
    </code>
    <br>
    ${
        try {
            @Suppress("UNCHECKED_CAST") // we're pretty sure it's a `List<Note>`
            response.data.let { notes -> 
            """
            <p>${notes.size} note${addPostfixS(notes)} found for user: ${request.queryParameters["email"]}</p>
            
            <style>
                .italic {
                    font-style: italic; 
                    font-weight: 100;
                }
                ul li::before {
                  content: counter(items) "\2022";
                  color: #BBBBBB;
                  font-weight: bold;
                  display: inline-block; 
                  width: 1em;
                  margin-left: -1em;
                  counter-increment: items;
                }
            </style>
            <ul style="counter-reset: items;">
            ${
                // List each note as a list item
                notes.map { note ->
            """
                <li style="background-color: ${prependHashIfNotPresent(note.color)};
                           color: ${prependHashIfNotPresent(invertColor(note.color, true))};
                           list-style: none;
                           list-style-color: #BBBBBB;
                           margin: 4px 2px;"
                    >
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
            } 
        } catch (e: Exception) {  // just in case the cast fails
            e.printStackTrace()
            "<br><p style=\"color:red; background-color:black;\">" +
                    "an error occurred ${e.localizedMessage}</p><br>" +
                    "<p><code>${e.stackTrace.joinToString("<br>")}</code></p>"
        }
    }                                   
    """.trimIndent()



//// UTILS /////

// Add s to the end of the string if it's greater than 1
private fun addPostfixS(it: List<Note>) =
    if (it.size > 1) "s" else ""

private fun prependHashIfNotPresent(it: String) =
    if(it.startsWith("#")) it else "#$it"

private fun removeHashIfPresent(it: String) =
    if(it.startsWith("#")) it.substring(1) else it

// Accepts a hex color string (with or without "#" prefix) and returns
//   a "#"-prefixed hex color string representing the inverted color.
private fun invertColor(colorHexStr: String, forceLightOrDark: Boolean): String {
    // conform the color string to a "#RRGGBB" hex string
    val colorHex = prependHashIfNotPresent(colorHexStr).substring(1)

    val r = Integer.parseInt(colorHex.substring(0, 2), 16)
    val g = Integer.parseInt(colorHex.substring(2, 4), 16)
    val b = Integer.parseInt(colorHex.substring(4, 6), 16)
    val a = if(colorHex.length>6) Integer.parseInt(colorHex.substring(6, 8), 16) else 255

    return if (forceLightOrDark) {
        if (((r + g + b)/3.0) < 128 || a < 128 ) "#DDDDDD" else "#222222"
    } else {
        "#${String.format("%02x%02x%02x", 255 - r, 255 - g, 255 - b)}FF"  // Forces alpha to be 255
        // return "#${String.format("%02X", r)}${String.format("%02X", g)}${String.format("%02X", b)}"
    }
}

