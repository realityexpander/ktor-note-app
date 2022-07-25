package com.realityexpander

// Ktor project generator
// https:/start.ktor.io/

// Ktor Docs
// https://ktor.io/docs/requests.html#path_parameters

// LetsEncypt - signs certificates for a domain for free (but we don't have a domain yet)
// https://letsencrypt.org/certificates/

// --> For SSL/HTTPS traffic.
// Self-generate certificates (.jks) for our server app (won't be accepted by general websites, but good for our testing)
// Set up the server folder structure locally. From root folder:
//   take root    # create the `/root` folder locally (not used on the server)
//   take home
//   take keys
//   keytool -genkey -v -keystore ktornoteapp.jks -alias ktor_note_app -keyalg RSA -keysize 4096
//     -> password: <**ENTER_KEYSTORE_PASSWORD***>    <-- keyStorePassword
//     -> first and lastname: chris athanas
//     -> organization unit: realityexpander
//     -> organization: realityexpander
//     -> city: austin
//     -> state: texas
//     -> country code: us
//     -> correct: yes
//     -> enter key password: <**PRIVATE_KEY_PASSWORD_NOT_NECESSARY**>    <-- privateKeyPassword (may be same as keystore password)
//     -> enter key password again: <**PRIVATE_KEY_PASSWORD_NOT_NECESSARY**>

// --> For LOGIN to server.
// To generate SSH keys for the login to server (so you don't have to type in server password every time)
// On local terminal:
// Generate SSH keys:
//   ssh-keygen -t rsa -m PEM
//    Enter file in which to save the key (/Users/chrisathanas/.ssh/id_rsa):
//    # Enter this (the location to save the .ssh key file):
//      /Users/chrisathanas/.ssh/hostinger_rsa
//    # Press enter twice (no passphrase needed)
//    Your public key has been saved in /Users/chrisathanas/.ssh/hostinger_rsa.pub
//
//   cat /Users/chrisathanas/.ssh/hostinger_rsa.pub                          # This is the public key. We need to copy the private (non-.pub) to the server.
//   ssh-copy-id -i /Users/chrisathanas/.ssh/hostinger_rsa root@<server-ip>  # copy the private key to the server (must use password to login)
//   ssh -i /Users/chrisathanas/.ssh/hostinger_rsa root@<server-ip>          # connect to the server using the private key
//
// To SSH into the server without long reference to `.../.ssh/hostinger_rsa`, do the following:
//   nano ~/.ssh/config  # & Add these lines: (to allow ssh/scp/sftp to work without supplying password)
//
//     Host <shortcut-name>
//       Host <server-ip-address>
//       User root
//       IdentityFile ~/.ssh/hostinger_rsa

// Secure FTP (sftp) commands:
// Put folders recursively:
//   put -R *

// Tree (tree) commands:
//   tree -phD = show all files and folders in the current directory with -la and human-readable size & Date Modified
//   tree -saD = show hidden files & dates

// How to create an SSH shortcut
//   https://www.digitalocean.com/community/tutorials/how-to-create-an-ssh-shortcut

// Local dir structure (mirrored on the server):
// root      # this is the same as root folder on the server
//   home
//     keys
//       ktornoteapp.jks
//     release
//       app-0.X.X.jar
//     startApp.sh

// Deploy to an Ubuntu 20.04 (Focal) server: (also works with Ubuntu 18.04.6 LTS (GNU/Linux 4.15.0 x86_64))
//
// From root of project, create the FatJar file of our app:
//   ./gradle jar
// Run sftp to upload the jar of the app to the server:
//   sftp root@<ip address of server>
//   --> enter <server_password>
//   mkdir home
//   cd home
//   put app-0.0.1.jar
//
// Make a new tab in the terminal, to upload the keys to the server:
//   cd keys
//   sftp root@<ip address of server>:/home
//   --> enter <server_password>
//   mkdir keys
//   cd keys
//   put ktornoteapp.jks
//
// Install the app and mongo on our server:
// Make a new tab in the terminal.
//   ssh root@<ip address of server>
//   --> enter <server_password>
//   sudo apt-get update
//   cd /home
//   ls
// Should see our files
//   sudo apt-get install openjdk-8-jdk
//   --> Press enter to install java
//   java -version
//
// Install community edition of MongoDB:
//   https://www.mongodb.com/docs/manual/tutorial/install-mongodb-on-ubuntu/
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
//   ExecStart=/root/home/release/startApp.sh   // good shortcut for absolute path
//   // ExecStart=/usr/bin/java -jar /root/home/release/app-0.0.1.jar  // run an app of a specific version (** -> dont add this commented line)
//
//   [Install]
//   WantedBy=multi-user.target
//   ^s^x                              // Save and Exit
// Start the service:
//   sudo systemctl start ktornoteapp  // start the service
//   sudo systemctl restart ktornoteapp  // start the service
//   sudo systemctl status ktornoteapp // check if the service is running & show logs, 'q' to quit
//   sudo systemctl enable ktornoteapp // enable the service to start at boot time

// Show the logs & follow updates:
//   journalctl -f -u ktornoteapp
// Show the last 50 lines & follow updates:
//   tail -n 50 -f /var/log/syslog
// or:
//   journalctl -f -u ktornoteapp -n 50
// Clear the logs from all but last 2 days:
//   journalctl --vacuum-time=2d
//
// more options:
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
//
// to set the max size of the logs for the system:
//   sudo nano /etc/systemd/journald.conf
//     [Journal]
//     SystemMaxUse=50M
//   ^s^x

// Disable firewall: *note: May need to restart the app service after these commands.
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

// Check ip ports in use:
//   sudo netstat -peanut | grep ":8001"   # just look for port 8001
//   sudo lsof -i :27017
//   sudo lsof -i :8001
//   sudo lsof -i :8002
// Get command that started a particular process:
//   ps -fp <PID>
// Get PID of a command that started processes:
//   ps -ef | grep java | grep -v grep

// Add Traffic to and from mongod instances:
//   iptables -A INPUT -s <ip-address> -p tcp --destination-port 27017 -m state --state NEW,ESTABLISHED -j ACCEPT
//   iptables -A OUTPUT -d <ip-address> -p tcp --source-port 27017 -m state --state ESTABLISHED -j ACCEPT
// Remove traffic from mongod instances:
//   iptables -L --line-numbers     # lists both INPUT and OUTPUT
//   iptables -D INPUT <line-number-to-remove>

// Issues with connecting to mongodb:
//   https://www.mongodb.com/docs/compass/current/troubleshooting/connection-errors/
//   https://www.mongodb.com/docs/manual/reference/configuration-options/#mongodb-setting-net.bindIp

// To allow external access by MongoDB Compass to your server:
// Update the /etc/mongod.conf YAML file to allow external access:
//   sudo nano /etc/mongod.conf
//
//   # network interfaces
//   net:
//     port: 27017
//     bindIp: 127.0.0.1, <your-server-ip-address>
//   security:
//     authorization: enabled
//
// Restart the MongoDB service:
//   sudo systemctl restart mongod

// Log in to remote service with Mongo Compass:
//   New connection:
//   Select <Advanced Options> twirl-down
//   Select the <Proxy/SSH> tab
//   Click the "SSH with Password" button
//   Enter the following:
//    SSH Hostname: <your-server-ip-address>
//    SSH Port: 22
//    SSH Username: <your-SSH-username>   // usually 'root'
//    SSH Password: <your-SSH-password>   // <your-SSH-password>
//   Select the "Authentication" tab
//   Enter the following:
//    Username: <your-admin-username>     // usually 'theAdmin'
//    Password: <your-admin-password>     // <your-admin-password>
//    Authentication Database: admin      // usually 'admin'
//    Authentication Mechanism: Default   // usually 'Default'

// Monitor IP traffic in real time:  https://linuxize.com/post/linux-watch-command/
//   watch -d --interval 0 'iptables -nvL | grep -v "0     0"'
//   watch -d=cumulative # Keep highlighting values that have changed

// To remove the server warning: setlocale: LC_ALL: cannot change locale (en_US.UTF-8):
//   locale-gen en_US.utf8

// Kill a user session: (log the user out)
//   sudo pkill -u <username>

// Build commands
//   Build and upload app to server:
//    ./gradle buildAndUploadLatestReleaseWithKeys
//   Build jar only:
//    ./gradle jar
//   Show app manifest:
//    ./gradle getAppManifest

// GOTTA BE CAREFUL WITH THE KEYS! Make sure you never check in your *.jks file to GitHub!
//   ALSO give your mongoDb a password!
//
// All your data is a backed up. You must pay 0.043 BTC to 1Kz6v4B5CawcnL8jrUvHsvzQv5Yq4fbsSv
// 48 hours for recover it. After 48 hours expiration we will leaked and exposed all your data.
// In case of refusal to pay, we will contact the General Data Protection Regulation, GDPR and
// notify them that you store user data in an open form and is not safe. Under the rules of the law,
// you face a heavy fine or arrest and your base dump will be dropped from our server!
// You can buy bitcoin here, does not take much time to buy https://localbitcoins.com or
// https://buy.moonpay.io/ After paying write to me in the mail with your DB
// IP: recmydb+1t55h@onionmail.org and/or mariadb@mailnesia.com and you will receive a
// link to download your database dump.

// Web url to get all notes for a specific user:
//   http://localhost:8001/getAllNotesForEmail?email=test123@123.com

// https://www.netscylla.com/blog/2018/09/13/hacking-mongodb.html
// MongoDB commands:
//help                         ;show help
//show dbs                     ;show database names
//show collections             ;show collections in current database
//show users                   ;show users in current database
//show profile                 ;show most recent system.profile entries with time >= 1ms
//use <db name>                ;set curent database to <db name>
//
//db.addUser (username, password)
//db.removeUser(username)
//
//db.cloneDatabase(fromhost)
//db.copyDatabase(fromdb, todb, fromhost)
//db.createCollection(name, { size : ..., capped : ..., max : ... } )
//db.getName()
//db.printCollectionStats()
//db.currentOp() ;displays the current operation in the db
//db.getProfilingLevel()
//db.setProfilingLevel(level) ;0=off 1=slow 2=all
//db.getReplicationInfo()
//db.printReplicationInfo()
//db.printSlaveReplicationInfo()
//db.repairDatabase()
//
//db.version() ;current version of the server
//
// https://www.mongodb.com/docs/manual/tutorial/configure-scram-client-authentication/
// https://www.guru99.com/mongodb-create-user.html
// Create an admin user:
//  use admin
//  db.createUser({user: "theAdmin", pwd: "password", roles: [{role: "readWrite", db: "admin"}]})
// or:
//  db.createUser({user: "theAdmin", pwd: passwordPrompt(),
//    roles: [
//      { role: "userAdminAnyDatabase", db: "admin" },
//      { role: "readWriteAnyDatabase", db: "admin" }
//  ]})
// Auth the user:
//  use admin
//  db.auth("theAdmin", passwordPrompt()) // or cleartext password
// Show users:
//  use admin
//  db.system.users.find()
// Remove user:
//  use admin
//  db.system.users.deleteOne({user: "user"})


import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import com.realityexpander.data.checkPasswordForEmail
import com.realityexpander.data.printMongoEnv
import com.realityexpander.routes.loginRoute
import com.realityexpander.routes.notesRoute
import com.realityexpander.routes.registerRoute
import com.realityexpander.routes.styleRoute
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.css.CssBuilder
import org.slf4j.LoggerFactory
import kotlin.time.ExperimentalTime

fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

@OptIn(ExperimentalTime::class)
@Suppress("unused") // Referenced in application.conf
@kotlin.jvm.JvmOverloads
fun Application.module(testing: Boolean = false) {

    install(DefaultHeaders)      // Add default headers (ie: Date of the request)
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

    install(Routing) {           // Our routes are defined in `/routes`
        styleRoute()
        registerRoute()
        loginRoute()
        notesRoute()
    }

    // Configure the mongo database logging
    val loggerContext = LoggerFactory.getILoggerFactory() as LoggerContext
    val rootLogger = loggerContext.getLogger("org.mongodb.driver")
    rootLogger.level = Level.WARN

//    // Test to see if cyclic job can run
//    val scheduledEventFlow = flow{
//        while(true){
//            delay(10000)
//            emit(true)
//        }
//    }
//    scheduledEventFlow.onEach{ myLittleJob() }.launchIn(this)


//    Testing
//    CoroutineScope(Dispatchers.IO).launch {
//        println("Email Exists = ${checkIfUserExists("test@123.com")}")
//    }


    // Print mongo environment variables - For debugging only
    // printMongoEnv()

}

var i = 0
fun myLittleJob(){
    println("Jello world ${++i}")
}

// Response with CSS
suspend inline fun ApplicationCall.respondCss(builder: CssBuilder.() -> Unit) {
    respondText(CssBuilder().apply(builder).toString(), ContentType.Text.CSS)
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