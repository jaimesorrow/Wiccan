package com.nyxwell.app.data.repository

import com.google.firebase.functions.FirebaseFunctions
import com.nyxwell.app.data.local.CachedDailyContent
import com.nyxwell.app.data.local.CachedDailyContentDao
import com.nyxwell.app.domain.model.Content
import com.nyxwell.app.domain.model.ContentStatus
import com.nyxwell.app.domain.model.ContentTier
import com.nyxwell.app.domain.model.ContentType
import com.nyxwell.app.domain.repository.ContentRepository
import com.nyxwell.app.domain.result.AppResult
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@Singleton
class FirebaseContentRepository @Inject constructor(
    private val functions: FirebaseFunctions,
    private val cachedDailyContentDao: CachedDailyContentDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ContentRepository {
    override suspend fun refreshTodayContent(userId: String): AppResult<Content> = withContext(ioDispatcher) {
        runCatching {
            val response = functions
                .getHttpsCallable("getTodayContent")
                .call(mapOf("uid" to userId))
                .await()
            response.data.toContent()
        }.fold(
            onSuccess = { content ->
                cachedDailyContentDao.upsert(content.toCachedDailyContent(currentIsoDate()))
                AppResult.Success(content)
            },
            onFailure = { throwable -> AppResult.Error(throwable) },
        )
    }

    override fun observeTodayContent(): Flow<Content?> =
        cachedDailyContentDao.observeTodayContent().map { cached ->
            cached
                ?.takeIf { it.cachedDate == currentIsoDate() }
                ?.toDomain()
        }
}

private fun Any?.toContent(): Content {
    val payload = this as? Map<*, *> ?: throw IllegalArgumentException("Expected content map payload")

    fun requiredString(key: String): String = payload[key] as? String
        ?: throw IllegalArgumentException("Missing $key in content payload")

    fun requiredStringList(key: String): List<String> = (payload[key] as? List<*>)?.map { item ->
        item as? String ?: throw IllegalArgumentException("Invalid list entry for $key")
    } ?: throw IllegalArgumentException("Missing $key in content payload")

    fun optionalStringList(key: String): List<String> =
        (payload[key] as? List<*>)?.map { item ->
            item as? String ?: throw IllegalArgumentException("Invalid list entry for $key")
        }.orEmpty()

    return Content(
        id = requiredString("id"),
        type = ContentType.fromWireValue(requiredString("type")),
        tier = ContentTier.fromWireValue(requiredString("tier")),
        tradition = requiredString("tradition"),
        title = requiredString("title"),
        intro = requiredString("intro"),
        body = requiredString("body"),
        materials = optionalStringList("materials"),
        safetyNote = payload["safetyNote"] as? String,
        tags = requiredStringList("tags"),
        status = ContentStatus.fromWireValue(requiredString("status")),
    )
}

private fun Content.toCachedDailyContent(cachedDate: String): CachedDailyContent = CachedDailyContent(
    id = id,
    type = type,
    tier = tier,
    tradition = tradition,
    title = title,
    intro = intro,
    body = body,
    materials = materials,
    safetyNote = safetyNote,
    tags = tags,
    status = status,
    cachedDate = cachedDate,
)

private fun CachedDailyContent.toDomain(): Content = Content(
    id = id,
    type = type,
    tier = tier,
    tradition = tradition,
    title = title,
    intro = intro,
    body = body,
    materials = materials,
    safetyNote = safetyNote,
    tags = tags,
    status = status,
)

private fun currentIsoDate(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).run {
    timeZone = TimeZone.getTimeZone("UTC")
    format(Date())
}
