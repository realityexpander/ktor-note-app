package com.realityexpander

import com.realityexpander.data.checkIfUserExists
import com.realityexpander.data.collections.User
import com.realityexpander.data.registerUser
import com.realityexpander.routes.loginRoute
import com.realityexpander.routes.registerRoute
import io.ktor.application.*
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


//    Testing
//    CoroutineScope(Dispatchers.IO).launch {
//        println("Email Exists = ${checkIfUserExists("test@123.com")}")
//    }

}

