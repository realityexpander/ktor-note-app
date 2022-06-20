package com.realityexpander.data.requests

data class AddOwnerToNoteRequest(
    val noteId: String,
    val ownerIdToAdd: String
)
