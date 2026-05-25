package dev.mangolabs.quilleditor.model

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DeltaOpExtractorsTest {

  private val json = Json { ignoreUnknownKeys = true }

  @Test
  fun textInsertReturnsStringForTextOp() {
    val op = json.decodeFromString<DeltaOp>("""{"insert":"hello"}""")
    assertEquals("hello", op.textInsert)
  }

  @Test
  fun textInsertReturnsNullForEmbedOp() {
    val op = json.decodeFromString<DeltaOp>("""{"insert":{"image":"foo"}}""")
    assertNull(op.textInsert)
  }

  @Test
  fun imageUrlReturnsStringForImageEmbed() {
    val op = json.decodeFromString<DeltaOp>("""{"insert":{"image":"app-image://abc"}}""")
    assertEquals("app-image://abc", op.imageUrl)
  }

  @Test
  fun imageUrlReturnsNullForTextOp() {
    val op = json.decodeFromString<DeltaOp>("""{"insert":"plain"}""")
    assertNull(op.imageUrl)
  }

  @Test
  fun imageUrlReturnsNullForNonImageEmbed() {
    val op = json.decodeFromString<DeltaOp>("""{"insert":{"video":"foo"}}""")
    assertNull(op.imageUrl)
  }
}
