package dev.mangolabs.quilleditor.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class ActiveFormat(
  val bold: Boolean = false,
  val italic: Boolean = false,
  val underline: Boolean = false,
  val strike: Boolean = false,
  val list: String? = null,
  val header: Int? = null
) {
  companion object {
    private val json = Json { ignoreUnknownKeys = true }

    fun fromJson(jsonString: String): ActiveFormat {
      val obj = json.parseToJsonElement(jsonString).jsonObject

      // Quill may emit `false` for cleared list / header formats — treat as null.
      val listPrim = obj["list"]?.jsonPrimitive
      val list = when {
        listPrim == null -> null
        listPrim.booleanOrNull == false -> null
        else -> listPrim.contentOrNull
      }
      val headerPrim = obj["header"]?.jsonPrimitive
      val header = when {
        headerPrim == null -> null
        headerPrim.booleanOrNull == false -> null
        else -> headerPrim.intOrNull
      }

      return ActiveFormat(
        bold = obj["bold"]?.jsonPrimitive?.booleanOrNull ?: false,
        italic = obj["italic"]?.jsonPrimitive?.booleanOrNull ?: false,
        underline = obj["underline"]?.jsonPrimitive?.booleanOrNull ?: false,
        strike = obj["strike"]?.jsonPrimitive?.booleanOrNull ?: false,
        list = list,
        header = header
      )
    }
  }
}
