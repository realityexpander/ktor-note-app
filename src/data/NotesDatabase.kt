package com.realityexpander.data

import com.realityexpander.data.collections.Note
import com.realityexpander.data.collections.User
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

suspend fun getUser(email: String): User? {
    return users.findOne(User::email eq email)
}

suspend fun checkPasswordForEmail(email: String, passwordToCheck: String): Boolean {
    return users.findOne(User::email eq email)
        ?.password == passwordToCheck
}

suspend fun getNotesForUser(email: String): List<Note> {
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
    return notes.find(Note::owners contains id).toList() // text based query
}


suspend fun saveNote(note: Note): Boolean {
    if(note.id == null || note.id.isBlank()) {
        return notes.insertOne(note).wasAcknowledged() // inserting will automatically set the id of the new note
    }

    val noteExists = notes.findOneById(note.id) != null

    return if (noteExists) {
        notes.updateOneById(note.id, note).wasAcknowledged()
    } else {
        notes.insertOne(note).wasAcknowledged()  // if note id is supplied, but doesn't exist, insert it anyway
    }
}

suspend fun deleteNoteForUserId(userId: String, noteId: String): Boolean {
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

suspend fun getEmailForUserId(userId: String): String? {
    return users.findOneById(userId)?.email
}