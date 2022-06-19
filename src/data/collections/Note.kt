package com.realityexpander.data.collections

import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

data class Note(
    @BsonId // binary json object id marker
    val id: String = ObjectId().toString(),
    val title: String,
    val content: String,
    val date: String,
    val owners: List<String>,
    val color: String,
)
