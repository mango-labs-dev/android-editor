package dev.mangolabs.quilleditor.model

import kotlinx.serialization.SerializationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DeltaTest {

  private val json = Json { ignoreUnknownKeys = true }

  @Test
  fun textInsertOpsRoundTrip() {
    val source = """{"ops":[{"insert":"Hello "},{"insert":"world","attributes":{"bold":true}}]}"""
    val delta = json.decodeFromString<Delta>(source)
    assertEquals(2, delta.ops.size)
    assertEquals("Hello ", delta.ops[0].textInsert)
    assertEquals("world", delta.ops[1].textInsert)
    assertNotNull(delta.ops[1].attributes)
    val encoded = json.encodeToString(delta)
    assertEquals(delta, json.decodeFromString<Delta>(encoded))
  }

  @Test
  fun imageEmbedRoundTrips() {
    val source = """{"ops":[{"insert":{"image":"app-image://abc"}}]}"""
    val delta = json.decodeFromString<Delta>(source)
    assertEquals(1, delta.ops.size)
    assertEquals("app-image://abc", delta.ops[0].imageUrl)
    assertNull(delta.ops[0].textInsert)
    assertEquals(delta, json.decodeFromString<Delta>(json.encodeToString(delta)))
  }

  @Test
  fun deleteAndRetainOpsRoundTrip() {
    val source = """{"ops":[{"retain":5},{"delete":3},{"insert":"x"}]}"""
    val delta = json.decodeFromString<Delta>(source)
    assertEquals(3, delta.ops.size)
    assertEquals(5, delta.ops[0].retain)
    assertEquals(3, delta.ops[1].delete)
    assertEquals("x", delta.ops[2].textInsert)
    assertEquals(delta, json.decodeFromString<Delta>(json.encodeToString(delta)))
  }

  @Test
  fun attributesPreserveUnknownKeys() {
    val source = """{"ops":[{"insert":"x","attributes":{"customAttr":42,"link":"https://example.com"}}]}"""
    val delta = json.decodeFromString<Delta>(source)
    val attrs = delta.ops[0].attributes
    assertNotNull(attrs)
    assertTrue(attrs!!.containsKey("customAttr"))
    assertTrue(attrs.containsKey("link"))
  }

  @Test
  fun emptyDeltaYieldsEmptyOpsList() {
    val delta = json.decodeFromString<Delta>("""{"ops":[]}""")
    assertEquals(0, delta.ops.size)
  }

  @Test
  fun missingOpsFieldDefaultsToEmptyList() {
    val delta = json.decodeFromString<Delta>("{}")
    assertEquals(0, delta.ops.size)
  }

  @Test(expected = SerializationException::class)
  fun malformedJsonThrowsSerializationException() {
    json.decodeFromString<Delta>("{not valid json")
  }
}
