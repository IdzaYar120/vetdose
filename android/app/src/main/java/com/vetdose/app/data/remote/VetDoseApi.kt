package com.vetdose.app.data.remote

import com.vetdose.app.data.remote.dto.SyncResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface VetDoseApi {
    /**
     * Mirrors `GET /api/v1/sync?since=`. Omitting [since] (null) requests a
     * full sync — used for the very first sync, and by the web/iOS client
     * whenever its IndexedDB turns out empty (Safari can clear storage).
     */
    @GET("api/v1/sync")
    suspend fun sync(@Query("since") since: String? = null): SyncResponseDto
}
