package com.realityexpander.data

import com.realityexpander.data.collections.Note
import com.realityexpander.data.collections.User
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
    // SELECT * FROM user WHERE email = :$email  // SQL would look like this

    // return users.findOne("{ email: '$email' }") != null  // text based query

    if(users.findOne("{ email: '$email' }") != null) {
     println("User exists")
    }

    return users.findOne(User::email eq email) != null // object based query
}

suspend fun checkPasswordForEmail(email: String, passwordToCheck: String): Boolean {
    return users.findOne(User::email eq email)?.password == passwordToCheck
}