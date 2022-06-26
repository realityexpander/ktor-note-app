package com.realityexpander

// Ktor Docs
// https://ktor.io/docs/requests.html#path_parameters

// LetsEncypt - signs certificates for a domain for free (but we dont have a domain yet)
// https://letsencrypt.org/certificates/

// Self-generate certificates for our ip Addresses (wont be accepted by general web sites, but good for our testing)
// For SSL/HTTPS traffic
// keytool -genkey -v -keystore ktornoteapp.jks -alias ktor_note_app -keyalg RSA -keysize 4096
//   -> password: password    <-- keyStorePassword
//   -> first and lastname: chris athanas
//   -> organization unit: realityexpander
//   -> organization: realityexpander
//   -> city: austin
//   -> state: texas
//   -> country code: us
//   -> correct: yes
//   -> enter password: password    <-- privateKeyPassword
//   -> enter password again: password

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import com.realityexpander.data.checkPasswordForEmail
import com.realityexpander.routes.loginRoute
import com.realityexpander.routes.notesRoute
import com.realityexpander.routes.registerRoute
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.routing.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.ExperimentalTime

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@OptIn(ExperimentalTime::class)
@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {

    install(DefaultHeaders)      // Add default headers
    install(CallLogging)         // log call details
    install(ContentNegotiation){  // serialize JSON
        gson {
            setPrettyPrinting()
        }
    }

    // Must set up authentication before setting up the Routes (or will crash)
    install(Authentication) {
        configureAuth()
    }

    install(Routing) {           // Our routes are defined in routes.kt
        registerRoute()
        loginRoute()
        notesRoute()
    }

    // Configure the mongo database logging
    val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
    val rootLogger = loggerContext.getLogger("org.mongodb.driver")
    rootLogger.level = Level.WARN

    val scheduledEventFlow = flow{
        while(true){
            delay(10000)
            emit(true)
        }
    }

    scheduledEventFlow.onEach{ myLittleJob() }.launchIn(this)


//    Testing
//    CoroutineScope(Dispatchers.IO).launch {
//        println("Email Exists = ${checkIfUserExists("test@123.com")}")
//    }

}

var i = 0
fun myLittleJob(){
    println("Jello world ${++i}")
}

// Setup "basic" authentication using email and password
private fun Authentication.Configuration.configureAuth() {
    basic {
        realm = "Note Server"
        validate { credentials ->
            val email = credentials.name
            val password = credentials.password

            // Check in database if user exists & password is correct
            if (checkPasswordForEmail(email, password)) {
                UserIdPrincipal(email)
            } else {
                null
            }
        }
    }
}