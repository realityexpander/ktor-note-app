package com.realityexpander.routes

import com.realityexpander.data.ifUserEmailExists
import com.realityexpander.data.collections.User
import com.realityexpander.data.registerUser
import com.realityexpander.data.requests.AccountRequest
import com.realityexpander.data.responses.SimpleResponse
import com.realityexpander.security.getHashWithSaltForPassword
import io.ktor.application.*
import io.ktor.http.HttpStatusCode.Companion.BadRequest
import io.ktor.http.HttpStatusCode.Companion.Conflict
import io.ktor.http.HttpStatusCode.Companion.Created
import io.ktor.http.HttpStatusCode.Companion.ExpectationFailed
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import io.ktor.http.HttpStatusCode.Companion.PreconditionFailed
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
                call.respond(
                    ExpectationFailed,
                    SimpleResponse(false, ExpectationFailed, message = "Error: ${e.localizedMessage}")
                )
                return@post
            } catch (e: Exception) {
                call.respond(
                    BadRequest,
                    SimpleResponse(false, BadRequest, message = "Error: ${e.localizedMessage}")
                )
                return@post
            }

            val userExists = ifUserEmailExists(request.email)
            if (!userExists) {

                if (request.email.isBlank() || request.password.isBlank()) {
                    call.respond(
                        PreconditionFailed,
                        SimpleResponse(false, PreconditionFailed, message = "Error: Email or password is blank")
                    )
                    return@post
                }

                if (registerUser(
                        User(email = request.email, password = getHashWithSaltForPassword(request.password))
                    )
                ) {
                    call.respond(
                        Created,
                        SimpleResponse(true, Created, message = "User registered successfully")
                    )
                } else {
                    call.respond(
                        InternalServerError,
                        SimpleResponse(false, InternalServerError, message = "Error: User could not be registered")
                    )
                }
            } else {
                call.respond(
                    Conflict,
                    SimpleResponse(false, Conflict, message = "Error: User/Email already exists")
                )
            }
        }
    }
}