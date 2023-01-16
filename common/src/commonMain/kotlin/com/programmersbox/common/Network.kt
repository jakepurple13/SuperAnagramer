package com.programmersbox.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Composable
internal fun highScores(vararg key: Any?) = produceState<Result<Scores>>(Result.Loading, keys = key) {
    value = Result.Loading
    val response = getHighScores().getOrNull()
    value = response?.let { Result.Success(it) } ?: Result.Error
}

internal sealed class Result<out R> {
    class Success<out T>(val value: T) : Result<T>()
    object Error : Result<Nothing>()
    object Loading : Result<Nothing>()
}

internal suspend fun getLetters() = runCatching {
    val client = HttpClient()
    val json = Json {
        isLenient = true
        prettyPrint = true
        ignoreUnknownKeys = true
        coerceInputValues = true
    }
    val response: HttpResponse = client.get("https://github.com/jakepurple13/WordSolver/blob/master/app/src/main/res/raw/words.txt?raw=true")
    val info = response.bodyAsText()
        .split("\n")
        .filter { it.all(Char::isLetter) }

    val word = info.filter { it.length == 7 }.random()

    Word(
        word = word,
        anagrams = info.filter { it.length >= 3 && it isAnagramOf word }
            .map { it.uppercase() }
    )
}

internal infix fun String.isAnagramOf(word: String): Boolean = isAnagram(word, this)

internal fun isAnagram(word: String, anagram: String): Boolean {
    val c = word.groupBy { it.lowercaseChar() }.mapValues { it.value.size }
    val a = anagram.groupBy { it.lowercaseChar() }.mapValues { it.value.size }

    for (i in a) {
        c[i.key]?.let { if (it < i.value) return false } ?: return false
    }

    return true
}

internal suspend fun getWordDefinition(word: String) = runCatching {
    getApi<Definition>("http://0.0.0.0:8080/wordDefinition/$word")
}

internal suspend fun getHighScores() = runCatching {
    getApi<Scores>("http://0.0.0.0:8080/highScores")
}

internal suspend fun postHighScore(name: String, score: Int) = runCatching {
    postApi<Scores>("http://0.0.0.0:8080/highScore/$name/$score")
}

internal suspend inline fun <reified T> getApi(
    url: String,
    noinline headers: HeadersBuilder.() -> Unit = {}
): T? {
    val client = HttpClient {
        install(ContentNegotiation) {
            json(
                Json {
                    isLenient = true
                    prettyPrint = true
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                }
            )
        }
    }
    val response: HttpResponse = client.get(url) { headers(headers) }
    return response.body<T>()
}

internal suspend inline fun <reified T> postApi(
    url: String,
    noinline headers: HeadersBuilder.() -> Unit = {}
): T? {
    val client = HttpClient {
        install(ContentNegotiation) {
            json(
                Json {
                    isLenient = true
                    prettyPrint = true
                    ignoreUnknownKeys = true
                    coerceInputValues = true
                }
            )
        }
    }
    val response: HttpResponse = client.post(url) { headers(headers) }
    return response.body<T>()
}

@Serializable
internal data class Word(val word: String, val anagrams: List<String>)

@Serializable
internal data class Definition(val word: String, val definition: String)

@Serializable
internal data class Scores(val list: List<HighScore>)

@Serializable
internal data class HighScore(val name: String, val score: Int)