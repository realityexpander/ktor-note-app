package com.realityexpander.data.requests

data class AddOwnerIdToNoteIdRequest(
    val noteId: String,
    val ownerIdToAdd: String
)
