package com.example.data.network

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ArchiveApiService {
    @GET("advancedsearch.php")
    suspend fun search(
        @Query("q") query: String,
        @Query("output") output: String = "json",
        @Query("rows") rows: Int = 50,
        @Query("page") page: Int = 1
    ): ArchiveSearchResponse

    @GET("metadata/{identifier}")
    suspend fun getMetadata(
        @Path("identifier") identifier: String
    ): ArchiveMetadataResponse

    companion object {
        const val BASE_URL = "https://archive.org/"
    }
}
