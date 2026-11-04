package com.example.data.network

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ArchiveSearchResponse(
    @Json(name = "responseHeader") val responseHeader: SearchHeader,
    @Json(name = "response") val response: SearchResultBody
)

@JsonClass(generateAdapter = true)
data class SearchHeader(
    @Json(name = "status") val status: Int,
    @Json(name = "params") val params: Map<String, String>? = null
)

@JsonClass(generateAdapter = true)
data class SearchResultBody(
    @Json(name = "numFound") val numFound: Int,
    @Json(name = "start") val start: Int,
    @Json(name = "docs") val docs: List<ArchiveDoc>
)

@JsonClass(generateAdapter = true)
data class ArchiveDoc(
    @Json(name = "identifier") val identifier: String,
    @Json(name = "title") val title: String? = null,
    @Json(name = "creator") val creator: String? = null,
    @Json(name = "album") val album: String? = null,
    @Json(name = "mediatype") val mediatype: String? = null,
    @Json(name = "format") val format: List<String>? = null,
    @Json(name = "licenseurl") val licenseUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class ArchiveMetadataResponse(
    @Json(name = "metadata") val metadata: ArchiveMetadata,
    @Json(name = "files") val files: List<ArchiveFile>? = null
)

@JsonClass(generateAdapter = true)
data class ArchiveMetadata(
    @Json(name = "identifier") val identifier: String,
    @Json(name = "title") val title: String? = null,
    @Json(name = "creator") val creator: String? = null,
    @Json(name = "album") val album: String? = null,
    @Json(name = "licenseurl") val licenseUrl: String? = null,
    @Json(name = "publicdate") val publicDate: String? = null
)

@JsonClass(generateAdapter = true)
data class ArchiveFile(
    @Json(name = "name") val name: String,
    @Json(name = "source") val source: String? = null,
    @Json(name = "format") val format: String? = null,
    @Json(name = "length") val length: String? = null, // in seconds
    @Json(name = "size") val size: String? = null,   // in bytes
    @Json(name = "md5") val md5: String? = null,
    @Json(name = "crc32") val crc32: String? = null,
    @Json(name = "sha1") val sha1: String? = null
)
