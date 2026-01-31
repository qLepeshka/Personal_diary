package com.example.personaldiary

data class DiaryEntry(
    val id: Long,
    val title: String,
    val content: String,
    val imagePath: String?,
    val createdAt: String
)