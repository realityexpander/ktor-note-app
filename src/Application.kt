package com.realityexpander

// Ktor Docs
// https://ktor.io/docs/requests.html#path_parameters

// LetsEncypt - signs certificates for a domain for free (but we don't have a domain yet)
// https://letsencrypt.org/certificates/

// Self-generate certificates for our app and server (won't be accepted by general web sites, but good for our testing)
// For SSL/HTTPS traffic.
// From root folder:
//   take keys
//   keytool -genkey -v -keystore ktornoteapp.jks -alias ktor_note_app -keyalg RSA -keysize 4096
//     -> password: password    <-- keyStorePassword
//     -> first and lastname: chris athanas
//     -> organization unit: realityexpander
//     -> organization: realityexpander
//     -> city: austin
//     -> state: texas
//     -> country code: us
//     -> correct: yes
//     -> enter password: password    <-- privateKeyPassword
//     -> enter password again: password

// TODO: Add /keys & *.jks folder to .gitignore

// Deploy to an Ubuntu 20.04 (Focal) server:
//
// From root of project, run "gradle jar":
//   ./gradle jar
// Run sftp to upload the jar of the app to the server:
//   sftp root@<ip address of server>:/home
//   --> enter password
//   put app-0.0.1.jar
// Make a new tab in the terminal, to upload the keys to the server:
//   cd keys
//   sftp root@<ip address of server>:/home/keys
//   --> enter password
//   put ktornoteapp.jks
//
// Start the server:
// Make a new tab in the terminal.
//   ssh root@<ip address of server>
//   --> enter password
//   sudo apt-get update
//   cd /home
//   ls
// Should see our files
//   sudo apt-get install openjdk-8-jdk
//   --> Press enter to install java
//   java -version
// Install community edition of MongoDB:
// https://www.mongodb.com/docs/manual/tutorial/install-mongodb-on-ubuntu/
//   // wget -qO - https://www.mongodb.org/static/pgp/server-4.4.asc | sudo apt-key add -
//   wget -qO - https://www.mongodb.org/static/pgp/server-5.0.asc | sudo apt-key add -
// Reload the package database:
//   sudo apt-get update
// Install the MongoDB packages:
//   sudo apt-get install -y mongodb-org
// Start the MongoDB service:
//   sudo systemctl start mongod
//   sudo systemctl status mongod
// Install nano editor:
//   sudo apt-get install -y nano
// Create service configuration file for our app:
//   sudo nano /etc/systemd/system/ktornoteapp.service
//
//   [Unit]
//   Description=Ktor Note App service
//   After=mongod.service              // defines which service to start after
//   StartLimitIntervalSec=0           // prevents the service from not being started if it fails to start more than 5 times
//
//   [Service]
//   Type=simple
//   RestartSec=5                      // how long to wait before restarting the service
//   Restart=always                    // restart the service if it fails to start
//   User=root                         // run the service as root user
//   // ExecStart=/home/ktornoteapp.sh // good shortcut for absolute path (** -> dont add this commented line)
//   ExecStart=/usr/bin/java -jar /home/app-0.0.1.jar
//
//   [Install]
//   WantedBy=multi-user.target
//   ^s^x                              // Save and Exit
// Start the service:
//   sudo systemctl start ktornoteapp  // start the service
//   sudo systemctl status ktornoteapp // check if the service is running & show logs, 'q' to quit
//   sudo systemctl enable ktornoteapp // enable the service to start at boot time

// Show the logs:
//   journalctl -u ktornoteapp.service // -f to show logs from the last 24 hours, 'q' to quit
//   journalctl -u ktornoteapp.service | grep -i error // grep for errors
//   journalctl -u ktornoteapp.service | grep -i error | tail -n 10 // show the last 10 errors
//   journalctl -u ktornoteapp.service | grep -i error | tail -n 10 | less // show the last 10 errors in a less window
//   journalctl -u ktornoteapp.service | grep -i error | tail -n 10 | less -R // show the last 10 errors in a less window, with readline support
//   journalctl -u ktornoteapp.service | grep -i error | tail -n 10 | less -R -S // show the last 10 errors in a less window, with readline support and scrollback
//   journalctl -u ktornoteapp.service | grep -i error | tail -n 10 | less -R -S -X // show the last 10 errors in a less window, with readline support and scrollback and exit on 'q'
//   journalctl -u ktornoteapp.service | grep -i error | tail -n 10 | less -R -S -X -M // show the last 10 errors in a less window, with readline support and scrollback and exit on 'q' and show the cursor at the end of the file
//   journalctl -u ktornoteapp.service | grep -i error | tail -n 10 | less -R -S -X -M -F // show the last 10 errors in a less window, with readline support and scrollback and exit on 'q' and show the cursor at the end of the file and follow the logs
//   journalctl -u ktornoteapp.service | grep -i error | tail -n 10 | less -R -S -X -M -F -N // show the last 10 errors in a less window, with readline support and scrollback and exit on 'q' and show the cursor at the end of the file and follow the logs and dont show the cursor

// *note: May need to restart the app service after these commands.
// Disable firewall:
//   sudo apt-get install ufw
// Get status of firewall:
//   sudo ufw status
// Disable firewall:
//   sudo ufw disable
// Enable firewall:
//   sudo ufw enable
// Allow incoming connections:
//   sudo ufw allow <port>     // 8002 for our app
// Allow outgoing connections:
//   sudo ufw allow out <port> // 8002 for our app
// Allow incoming connections from a specific IP:
//   sudo ufw allow from <ip>
// Allow outgoing connections to a specific IP:
//   sudo ufw allow out from <ip>
// Allow incoming connections from a specific IP and port:
//   sudo ufw allow from <ip> port <port>
// Allow outgoing connections to a specific IP and port:
//   sudo ufw allow out to <ip> port <port>



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