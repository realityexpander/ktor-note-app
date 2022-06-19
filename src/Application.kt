package com.realityexpander

import com.realityexpander.data.checkIfUserExists
import com.realityexpander.data.checkPasswordForEmail
import com.realityexpander.data.collections.User
import com.realityexpander.data.registerUser
import com.realityexpander.routes.loginRoute
import com.realityexpander.routes.registerRoute
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.html.InputType

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {

    install(DefaultHeaders)      // Add default headers
    install(CallLogging)         // log call details
    install(Routing) {           // Our routes are defined in routes.kt
        registerRoute()
        loginRoute()
    }
    install(ContentNegotiation){  // serialize JSON
        gson {
            setPrettyPrinting()
        }
    }
    install(Authentication) {
        configureAuth()
    }


//    Testing
//    CoroutineScope(Dispatchers.IO).launch {
//        println("Email Exists = ${checkIfUserExists("test@123.com")}")
//    }

}

// Setup basic authentication using email and password
private fun Authentication.Configuration.configureAuth() {
    basic {
        realm = "Note Server"
        validate { credentials ->
            val email = credentials.name
            val password = credentials.password

            if (checkPasswordForEmail(email, password)) {
                UserIdPrincipal(email)
            } else {
                null
            }
        }
    }
}