package com.jaimesorrow.wiccan.data.repository

import com.jaimesorrow.wiccan.data.local.CachedDailyContentDao
import com.jaimesorrow.wiccan.data.local.CachedDailyContentEntity
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory stand-in for the Room DAO, mirroring its single-row semantics. */
class FakeCachedDailyContentDao : CachedDailyContentDao {

    private val state = MutableStateFlow<CachedDailyContentEntity?>(null)

    override fun observe() = state

    override suspend fun get(): CachedDailyContentEntity? = state.value

    override suspend fun upsert(entity: CachedDailyContentEntity) {
        state.value = entity
    }

    override suspend fun clear() {
        state.value = null
    }
}
