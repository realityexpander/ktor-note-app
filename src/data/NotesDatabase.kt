package com.realityexpander.data

import com.realityexpander.data.collections.Note
import com.realityexpander.data.collections.User
import org.litote.kmongo.MongoOperator
import org.litote.kmongo.contains
import org.litote.kmongo.coroutine.coroutine
import org.litote.kmongo.eq
import org.litote.kmongo.reactivestreams.KMongo

private val client = KMongo.createClient().coroutine
private val database = client.getDatabase("Notes_Database")
private val users = database.getCollection<User>("Users")
private val notes = database.getCollection<Note>("Notes")

suspend fun registerUser(user: User): Boolean {
    return users.insertOne(user).wasAcknowledged()
}

suspend fun checkIfUserExists(email: String): Boolean {
    // SQL would look like this:
    // SELECT * FROM user WHERE email = :$email

    // json text based query:
    // return users.findOne("{ email: '$email' }") != null

    // object::based query
    return users.findOne(User::email eq email) != null
}

suspend fun checkPasswordForEmail(email: String, passwordToCheck: String): Boolean {
    return users.findOne(User::email eq email)
        ?.password == passwordToCheck
}

suspend fun getNotesForUser(email: String): List<Note> {
    val id = users.findOne(User::email eq email)?.id

    // json text based query
    //return notes.find("{ owners: { \$elemMatch: { \$eq: '$id' } } }").toList()

    // json text based query w/ mongo operators
    // return notes.find("""
    //  {
    //      'owners': {
    //          '${MongoOperator.elemMatch}': { '${MongoOperator.eq}': '$id'}
    //     }
    //  }
    //  """).toList()

    // object::based query
    return notes.find(Note::owners contains id).toList() // text based query
}