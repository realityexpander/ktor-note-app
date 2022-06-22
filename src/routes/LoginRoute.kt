package com.realityexpander.routes

import com.realityexpander.data.ifUserEmailExists
import com.realityexpander.data.checkPasswordForEmail
import com.realityexpander.data.requests.AccountRequest
import com.realityexpander.data.responses.SimpleResponse
import io.ktor.application.*
import io.ktor.html.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.html.*

fun Route.loginRoute() {
    post("/login") {
        var isFromWeb = false

        // Get the registration parameters
        val request = try {

            // From web or mobile app?
            if (call.request.contentType() == ContentType.Application.FormUrlEncoded) { // coming from web
                isFromWeb = true
                val formParameters = call.receiveParameters()

                formParameters.let {
                    AccountRequest(it["email"] ?: "", it["password"] ?: "")
                }
            } else {
                call.receive<AccountRequest>()  // coming from mobile app
            }
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

        val userExists = ifUserEmailExists(request.email)
        if (userExists) {
            if (checkPasswordForEmail(request.email, request.password)) {
                call.respondPlatform(
                    isFromWeb,
                    SimpleResponse(
                        true, HttpStatusCode.OK,
                        "Login successful"
                    )
                )
            } else {
                call.respondPlatform(
                    isFromWeb,
                    SimpleResponse(
                        false, HttpStatusCode.Unauthorized,
                        "Error: Password incorrect"
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

    get("/login") {
        respondLoginRawHTML(call)
        //respondLoginDslHtml(call)
    }
}

private suspend fun ApplicationCall.respondPlatform(
    isFromWeb: Boolean,
    response: SimpleResponse
) {
    when (isFromWeb) {
        true -> {
            respondRawHTML(response)
            //call.respondRedirect("/")
        }
        false -> {
            respond(response.statusCode, response)
        }
    }
}


private suspend fun respondLoginRawHTML(call: ApplicationCall) {
    call.respondHtml {
        unsafe {
            raw(
                """
                    <!DOCTYPE html>
                    <html>
                      <head>
                        <title>Login</title>
                        <style>
                            form {
                                background-color: #f0f0f0;
                            }
        
                            input {
                                font-size: 24px;
                            }
                        </style>
                      </head>
                      
                      <body>
                        <h1>Login</h1>
                        <form action="/login" method="post">
                            <br>
                            <input type="email" name="email" placeholder="Email">
                            
                            <br>
                            <br>
                            <input type="password" name="password" placeholder="Password">
                            
                            <br>
                            <br>
                            <input type="submit" name="Login">
                            
                            <br><br>
                        </form>
                      </body>
                    </html>
                """.trimIndent()
            )
        }
    }
}

private suspend fun respondLoginDslHtml(call: ApplicationCall) {
    call.respondHtml {
        head {
            title { +"Login" }
            style {
                unsafe {
                    raw(
                        """
                        form {
                            background-color: #f0f0f0;
                            font-size: 24px;
                        }

                        input {
                            font-size: 24px;
                        }
                        """.trimIndent()
                    )
                }
            }
        }
        body {
            h1 { +"Login" }
            form(action = "/login", method = FormMethod.post) {
                // Email
                //    div {
                //        label { +"Email" }
                //        input(type = InputType.email, name = "email") {
                //            placeholder = "Email"
                //        }
                //    }
                //    br {  }
                br { }
                input(type = InputType.email, name = "email") {
                    placeholder = "Email"
                }

                br { }
                br { }
                input(type = InputType.password, name = "password") {
                    placeholder = "Password"
                }
                br { }
                br { }
                input(type = InputType.submit, name = "Login")
                br { }
                br { }

            }
        }
    }
}

private suspend fun ApplicationCall.respondRawHTML(
    response: SimpleResponse = SimpleResponse(true, message = "Login successful")
) {
    respondHtml {
        unsafe {
            raw(
                """
                    <!DOCTYPE html>
                    <html>
                      <head>
                        <title>Login</title>
                        <style>
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
                                <p>${response.message}</p>
                                ${if (!response.isSuccessful) "<br><p>Response code: ${response.statusCode}</p>" else ""}
                                <br>
                            </div>
                        </h2>
                        <br>
                      </body>
                    </html>
                """.trimIndent()
            )
        }
    }
}
