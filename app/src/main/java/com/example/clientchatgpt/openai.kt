@file:OptIn(BetaOpenAI::class)

package com.example.clientchatgpt

import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletion
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.image.ImageCreation
import com.aallam.openai.api.image.ImageSize
import com.aallam.openai.api.logging.LogLevel
import com.aallam.openai.api.model.Model
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import java.lang.System.getenv

class ChatAI {
    private val token: String
    private val openAI: OpenAI
    private val memorySize = 6
    private var memory: MutableList<ChatMessage> = mutableListOf()


    constructor() {
        val apiKey = Secrets().getOpenAIKey("com.example.clientchatgpt")
        token = requireNotNull(apiKey) { "OPENAI_API_KEY environment variable must be set." }
        if (apiKey.isNullOrEmpty())
            println("ERROR: OPENAI_API_KEY is not found!!!")
        openAI = OpenAI(OpenAIConfig(token, LogLevel.All))
    }

    suspend fun gettingModels(): List<Model> {
        return openAI.models()
    }

    suspend fun doChat(request: String = "Hello!"): String {
        val currentMessage = ChatMessage(
            role = ChatRole.User,
            content = request
        )
        memory.add(currentMessage)
        val chatCompletionRequest = ChatCompletionRequest(
            model = ModelId("gpt-3.5-turbo"),
            messages = memory
        )
        val completion: ChatCompletion = openAI.chatCompletion(chatCompletionRequest)
        return if (completion.choices[0].message != null) {
            memory.add(completion.choices[0].message!!)
            while (memory.size > memorySize) {
                memory.removeAt(0)
            }
            completion.choices[0].message?.content.toString()
        } else {
            "<no answer>"
        }
// or, as flow
//        val completions: Flow<ChatCompletionChunk> = openAI.chatCompletions(chatCompletionRequest)
    }

    suspend fun doImage(
        request: String = "A cute baby sea otter",
        n: Int = 2,
        size: ImageSize = ImageSize.is1024x1024
    ): List<String> {
        val images = openAI.imageURL( // or openAI.imageJSON
            creation = ImageCreation(
                prompt = request,
                n = n,
                size = size
            )
        )

        return images.map { it.url }
    }

}

//[ImageURL(
//      url=https://oaidalleapiprodscus.blob.core.windows.net/private/org-URiNBeN60TjTKSFGZnNgUIJ6/user-cGu9fdGg4YNudJ4OYHE6kQPr/img-GNGMQhE7oetXP8bvqD04TbbI.png?st=2023-05-09T19%3A11%3A01Z&se=2023-05-09T21%3A11%3A01Z&sp=r&sv=2021-08-06&sr=b&rscd=inline&rsct=image/png&skoid=6aaadede-4fb3-4698-a8f6-684d7786b067&sktid=a48cca56-e6da-484e-a814-9c849652bcb3&skt=2023-05-09T19%3A50%3A15Z&ske=2023-05-10T19%3A50%3A15Z&sks=b&skv=2021-08-06&sig=dpM2I/4ybLC8//%2BMoJ8sasMJEW89RmwXZ4PLb7Fr/IA%3D),
// ImageURL(
//      url=https://oaidalleapiprodscus.blob.core.windows.net/private/org-URiNBeN60TjTKSFGZnNgUIJ6/user-cGu9fdGg4YNudJ4OYHE6kQPr/img-DVIjxJnTnyvAHR9MwUxTiUut.png?st=2023-05-09T19%3A11%3A01Z&se=2023-05-09T21%3A11%3A01Z&sp=r&sv=2021-08-06&sr=b&rscd=inline&rsct=image/png&skoid=6aaadede-4fb3-4698-a8f6-684d7786b067&sktid=a48cca56-e6da-484e-a814-9c849652bcb3&skt=2023-05-09T19%3A50%3A15Z&ske=2023-05-10T19%3A50%3A15Z&sks=b&skv=2021-08-06&sig=/uO4/ui0%2B96DfBjVYWpA0efg/RnHqtaBtZpSd9psJqk%3D)
// ]



// ChatCompletion(
// id=chatcmpl-7CtSAbXHAcIuoa3UqIyBrDjFq3rrl,
// created=1683307454,
// model=ModelId(id=gpt-3.5-turbo-0301),
// choices=[
//      ChatChoice(
//          index=0,
//          message=ChatMessage(
//              role=ChatRole(role=assistant),
//              content=! How can I assist you today?,
//          name=null),
//          finishReason=stop)],
// usage=com.aallam.openai.api.core.Usage@9633283)