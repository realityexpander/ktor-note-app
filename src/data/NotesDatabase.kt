package com.realityexpander.data

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.MongoCredential
import com.realityexpander.data.collections.Note
import com.realityexpander.data.collections.User
import com.realityexpander.security.isPasswordAndHashWithSaltMatching
import kotlinx.coroutines.runBlocking
import org.litote.kmongo.contains
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.eq
import org.litote.kmongo.reactivestreams.KMongo

// Get Environment Variables
val env: MutableMap<String, String> = System.getenv()
private val mongoSettings: Map<String, String> = mapOf(
    "MONGO_USERNAME"         to (env["MONGO_USERNAME"]         ?: ""),
    "MONGO_PASSWORD"         to (env["MONGO_PASSWORD"]         ?: ""),
    "MONGO_HOST"             to (env["MONGO_HOST"]             ?: "localhost"),
    "MONGO_HOST_POSTFIX"     to (env["MONGO_HOST_POSTFIX"]     ?: ""),
    "MONGO_PORT"             to (env["MONGO_PORT"]             ?: "27017"),
    "MONGO_AUTH_SOURCE"      to (env["MONGO_AUTH_SOURCE"]      ?: "admin"),
    "MONGO_DB"               to (env["MONGO_DB"]               ?: "Notes_Database"),
    "MONGO_USERS_COLLECTION" to (env["MONGO_USERS_COLLECTION"] ?: "Users"),
    "MONGO_NOTES_COLLECTION" to (env["MONGO_NOTES_COLLECTION"] ?: "Notes"),
)

// REFERENCE: server full client connection raw string (for app running on the server):
// from MongoCompass: mongodb://AdminUsername:PasswordXXXX%25%5E%26@localhost:27017/?authMechanism=DEFAULT&authSource=admin
//
// Use this way for raw connection string:
// MONGO_HOST=AdminUsername:PasswordXXXX%25%5E%26@localhost       // Note: uses ascii encoded username and password
// MONGO_HOST_POSTFIX=/?authSource=admin                   // Note: stripped out the "authmechanism" param

// Build the connection string
private val clientConnectionString =
    "mongodb://${mongoSettings["MONGO_HOST"]}" +
            ":${mongoSettings["MONGO_PORT"]}" +
            "${mongoSettings["MONGO_HOST_POSTFIX"]}"
private val credential = MongoCredential.createCredential(
    mongoSettings["MONGO_USERNAME"]!!,
    mongoSettings["MONGO_AUTH_SOURCE"]!!,
    mongoSettings["MONGO_PASSWORD"]!!.toCharArray()
)
private val client =
    MongoClientSettings
        .builder()
        .applyConnectionString(ConnectionString(clientConnectionString))
        .credential(credential)
        .build()

//private val mongoClient = KMongo.createClient(clientConnectionString).coroutine  // uses raw string
private val mongoClient = KMongo.createClient(client).coroutine
private val database = mongoClient.getDatabase(mongoSettings["MONGO_DB"]!!)
private val users = database.getCollection<User>(mongoSettings["MONGO_USERS_COLLECTION"]!!)
private val notes = database.getCollection<Note>(mongoSettings["MONGO_NOTES_COLLECTION"]!!)

fun printMongoEnv() {
    runBlocking {
        println("--- MongoDB Environment START ---")
        println("MONGO_USERNAME: ${mongoSettings["MONGO_USERNAME"]}")
        println("MONGO_PASSWORD: ${mongoSettings["MONGO_PASSWORD"]}")
        println("MONGO_HOST: ${mongoSettings["MONGO_HOST"]}")
        println("MONGO_HOST_POSTFIX: ${mongoSettings["MONGO_HOST_POSTFIX"]}")
        println("MONGO_AUTH_SOURCE: ${mongoSettings["MONGO_AUTH_SOURCE"]}")
        println("MONGO_PORT: ${mongoSettings["MONGO_PORT"]}")
        println("MONGO_DB: ${mongoSettings["MONGO_DB"]}")
        println("MONGO_USERS_COLLECTION: ${mongoSettings["MONGO_USERS_COLLECTION"]}")
        println("MONGO_NOTES_COLLECTION: ${mongoSettings["MONGO_NOTES_COLLECTION"]}")
        println()
        println("clientConnectionString: $clientConnectionString")
        println("credential: $credential")
        println()
        println("client username: ${client.credential?.userName}")
        println("client password: ${client.credential?.password.contentToString()}")
        println("database: ${database.name}")
        println("databases: ${mongoClient.listDatabaseNames().toList()}")
        println("users # of docs: ${users.countDocuments()}")
        println("notes # of docs: ${notes.countDocuments()}")
        println("--- MongoDB Environment END ---")
    }
}

suspend fun registerUser(user: User): Boolean {
    return users.insertOne(user).wasAcknowledged()
}

suspend fun ifUserEmailExists(email: String): Boolean {
    // SQL would look like this:
    // SELECT * FROM user WHERE email = :$email

    // json text based query:
    // return users.findOne("{ email: '$email' }") != null

    // object::based query
    return users.findOne(User::email eq email) != null
}

suspend fun ifUserIdExists(id: String): Boolean {
    return users.findOne(User::id eq id) != null
}

suspend fun getUserByEmail(email: String): User? {
    return users.findOne(User::email eq email)
}

suspend fun getEmailForUserId(userId: String): String? {
    return users.findOneById(userId)?.email
}

suspend fun checkPasswordForEmail(email: String, passwordToCheck: String): Boolean {
    val savedPassword = users.findOne(User::email eq email)
        ?: return false
    if (passwordToCheck == savedPassword.password) // TODO remove this, allows us to log into old accounts for now
        return true

    return isPasswordAndHashWithSaltMatching(passwordToCheck, savedPassword.password)
}

suspend fun getNotesForUserByEmail(email: String): List<Note> {
    val id = users.findOne(User::email eq email)?.id

    // json text based query:
    //return notes.find("{ owners: { \$elemMatch: { \$eq: '$id' } } }").toList()

    // json text based query w/ mongo operators:
    // return notes.find("""
    //  {
    //      'owners': {
    //          '${MongoOperator.elemMatch}': { '${MongoOperator.eq}': '$id'}
    //     }
    //  }
    //  """).toList()

    // object::based query:
    return notes.find(Note::owners contains id).toList()
}

suspend fun saveNote(note: Note): Boolean {
    if(note.id == null || note.id.isBlank()) {
        return notes.insertOne(note).wasAcknowledged() // inserting will automatically set the id of the new note
    }

    val noteExists = notes.findOneById(note.id) != null

    return if (noteExists) {
        note.updatedAt = System.currentTimeMillis()
        notes.updateOneById(note.id, note).wasAcknowledged()
    } else {
        note.createdAt = System.currentTimeMillis()
        notes.insertOne(note).wasAcknowledged()  // if note id is supplied, but doesn't exist, insert it anyway
    }
}

suspend fun deleteNoteIdForUserId(userId: String, noteId: String): Boolean {
    val note = notes.findOne(Note::id eq noteId, Note::owners contains userId)
        ?: run {
            println("Note with id=$noteId not found for User with id=$userId")
            return false
        }

    // Only one owner? Just delete the entire note.
    if(note.owners.size == 1) {
        return notes.deleteOneById(noteId).wasAcknowledged()
    }

    // More than one owner? Remove the owner id from the list.
    return notes.updateOneById(noteId, note.copy(owners = note.owners - userId)).wasAcknowledged()
}

suspend fun getNoteId(noteId: String): Note? {
    return notes.findOne(Note::id eq noteId)
}

suspend fun addOwnerIdToNoteId(userId: String, noteId: String): Boolean {
    val note = notes.findOneById(noteId) ?: run {
        println("Note with id=$noteId not found")
        return false
    }

    if (isOwnerOfNoteId(userId, noteId)) {
        println("User with id=$userId already owns note with id=$noteId")
        return false
    }

    return notes.updateOneById(noteId, note.copy(owners = note.owners + userId)).wasAcknowledged()
}

suspend fun isOwnerOfNoteId(userId: String, noteId: String): Boolean {
    val note = notes.findOneById(noteId) ?: run {
        println("Note with id=$noteId not found")
        return false
    }

    return note.owners.contains(userId)
}