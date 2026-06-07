package com.example.tarupramanata

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

data class ChatRequest(val message: String)
data class ChatResponse(val reply: String)

interface ChatApiService {
    @POST("chat")
    fun sendMessage(@Body request: ChatRequest): Call<ChatResponse>
}