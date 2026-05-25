package dev.mangolabs.quilleditor.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class Delta(val ops: List<DeltaOp> = emptyList())

@Serializable
data class DeltaOp(
  val insert: JsonElement? = null,
  val delete: Int? = null,
  val retain: Int? = null,
  val attributes: Map<String, JsonElement>? = null
)

val DeltaOp.textInsert: String?
  get() = (insert as? JsonPrimitive)?.takeIf { it.isString }?.contentOrNull

val DeltaOp.imageUrl: String?
  get() = (insert as? JsonObject)?.get("image")?.jsonPrimitive?.contentOrNull
