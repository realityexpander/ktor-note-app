package com.realityexpander.routes

import com.realityexpander.data.checkIfUserExists
import com.realityexpander.data.collections.User
import com.realityexpander.data.registerUser
import com.realityexpander.data.requests.AccountRequest
import com.realityexpander.data.responses.SimpleResponse
import io.ktor.application.*
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.Conflict
import io.ktor.http.HttpStatusCode.Companion.Created
import io.ktor.http.HttpStatusCode.Companion.NotAcceptable
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*

fun Route.registerRoute() {
    route("/") {
        get {
            call.respondText("Hello World!")
        }
    }

    route("/register") {
        post {

            // Get the registration paramters
            val request = try {
                 call.receive<AccountRequest>()
            } catch (e: ContentTransformationException) {
                call.respond(BadRequest, "Error: ${e.localizedMessage}")
                return@post
            } catch (e: Exception) {
                call.respond(NotAcceptable, "Error: ${e.localizedMessage}")
                return@post
            }

            val userExists = checkIfUserExists(request.email)
            if(!userExists) {
                if(registerUser(
                        User( email = request.email, password = request.password)
                    )
                ) {
                    call.respond(Created, SimpleResponse(true, "User registered successfully"))
                } else {
                    call.respond(NotAcceptable, SimpleResponse(false, "Error: User could not be registered"))
                }
            } else {
                call.respond(Conflict, SimpleResponse(false, "Error: User/Email already exists"))
            }
        }
    }
}