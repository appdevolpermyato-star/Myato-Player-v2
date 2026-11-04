package com.example.data.network

import com.squareup.moshi.FromJson
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonQualifier
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

@Retention(AnnotationRetention.RUNTIME)
@JsonQualifier
annotation class FlexibleString

class FlexibleStringAdapter {
    @FromJson
    @FlexibleString
    fun fromJson(reader: JsonReader): String? {
        return when (reader.peek()) {
            JsonReader.Token.STRING -> reader.nextString()
            JsonReader.Token.NUMBER -> reader.nextString()
            JsonReader.Token.BOOLEAN -> reader.nextBoolean().toString()
            JsonReader.Token.BEGIN_ARRAY -> {
                reader.beginArray()
                val list = mutableListOf<String>()
                while (reader.hasNext()) {
                    if (reader.peek() == JsonReader.Token.STRING || reader.peek() == JsonReader.Token.NUMBER) {
                        list.add(reader.nextString())
                    } else {
                        reader.skipValue()
                    }
                }
                reader.endArray()
                list.joinToString(", ")
            }
            JsonReader.Token.NULL -> reader.nextNull()
            else -> {
                reader.skipValue()
                null
            }
        }
    }

    @ToJson
    fun toJson(writer: JsonWriter, @FlexibleString value: String?) {
        writer.value(value)
    }
}

@JsonClass(generateAdapter = true)
data class ArchiveSearchResponse(
    @Json(name = "responseHeader") val responseHeader: SearchHeader? = null,
    @Json(name = "response") val response: SearchResultBody? = null
)

@JsonClass(generateAdapter = true)
data class SearchHeader(
    @Json(name = "status") val status: Int = 0,
    @Json(name = "params") val params: Map<String, String>? = null
)

@JsonClass(generateAdapter = true)
data class SearchResultBody(
    @Json(name = "numFound") val numFound: Int = 0,
    @Json(name = "start") val start: Int = 0,
    @Json(name = "docs") val docs: List<ArchiveDoc> = emptyList()
)

@JsonClass(generateAdapter = true)
data class ArchiveDoc(
    @Json(name = "identifier") val identifier: String,
    @FlexibleString @Json(name = "title") val title: String? = null,
    @FlexibleString @Json(name = "creator") val creator: String? = null,
    @FlexibleString @Json(name = "album") val album: String? = null,
    @FlexibleString @Json(name = "mediatype") val mediatype: String? = null,
    @Json(name = "format") val format: List<String>? = null,
    @FlexibleString @Json(name = "licenseurl") val licenseUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class ArchiveMetadataResponse(
    @Json(name = "metadata") val metadata: ArchiveMetadata? = null,
    @Json(name = "files") val files: List<ArchiveFile>? = null
)

@JsonClass(generateAdapter = true)
data class ArchiveMetadata(
    @Json(name = "identifier") val identifier: String,
    @FlexibleString @Json(name = "title") val title: String? = null,
    @FlexibleString @Json(name = "creator") val creator: String? = null,
    @FlexibleString @Json(name = "album") val album: String? = null,
    @FlexibleString @Json(name = "licenseurl") val licenseUrl: String? = null,
    @FlexibleString @Json(name = "publicdate") val publicDate: String? = null
)

@JsonClass(generateAdapter = true)
data class ArchiveFile(
    @Json(name = "name") val name: String,
    @FlexibleString @Json(name = "title") val title: String? = null,
    @FlexibleString @Json(name = "artist") val artist: String? = null,
    @FlexibleString @Json(name = "creator") val creator: String? = null,
    @FlexibleString @Json(name = "album") val album: String? = null,
    @FlexibleString @Json(name = "track") val track: String? = null,
    @Json(name = "source") val source: String? = null,
    @Json(name = "format") val format: String? = null,
    @Json(name = "length") val length: String? = null, // in seconds
    @Json(name = "size") val size: String? = null,     // in bytes
    @Json(name = "md5") val md5: String? = null,
    @Json(name = "crc32") val crc32: String? = null,
    @Json(name = "sha1") val sha1: String? = null
)
