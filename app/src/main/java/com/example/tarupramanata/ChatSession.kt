package com.example.tarupramanata

data class Message(
    val text: String,
    val isUser: Boolean
)

data class ChatSession(
    val id: String,
    val title: String,
    val date: String,
    val messages: MutableList<Message>
)