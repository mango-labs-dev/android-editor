package dev.mangolabs.quilleditor.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveFormatTest {

  @Test
  fun emptyObjectYieldsDefaults() {
    val f = ActiveFormat.fromJson("{}")
    assertEquals(ActiveFormat(), f)
  }

  @Test
  fun parsesAllBooleansTrue() {
    val f = ActiveFormat.fromJson("""{"bold":true,"italic":true,"underline":true,"strike":true}""")
    assertTrue(f.bold)
    assertTrue(f.italic)
    assertTrue(f.underline)
    assertTrue(f.strike)
  }

  @Test
  fun parsesAllBooleansFalse() {
    val f = ActiveFormat.fromJson("""{"bold":false,"italic":false,"underline":false,"strike":false}""")
    assertFalse(f.bold)
    assertFalse(f.italic)
    assertFalse(f.underline)
    assertFalse(f.strike)
  }

  @Test
  fun parsesListBullet() {
    assertEquals("bullet", ActiveFormat.fromJson("""{"list":"bullet"}""").list)
  }

  @Test
  fun parsesListOrdered() {
    assertEquals("ordered", ActiveFormat.fromJson("""{"list":"ordered"}""").list)
  }

  @Test
  fun listFalseMapsToNull() {
    assertNull(ActiveFormat.fromJson("""{"list":false}""").list)
  }

  @Test
  fun headerIntParses() {
    for (level in 1..6) {
      assertEquals(level, ActiveFormat.fromJson("""{"header":$level}""").header)
    }
  }

  @Test
  fun headerFalseMapsToNull() {
    assertNull(ActiveFormat.fromJson("""{"header":false}""").header)
  }

  @Test
  fun missingKeysDefaultToFalseOrNull() {
    val f = ActiveFormat.fromJson("""{"bold":true}""")
    assertTrue(f.bold)
    assertFalse(f.italic)
    assertFalse(f.underline)
    assertFalse(f.strike)
    assertNull(f.list)
    assertNull(f.header)
  }

  @Test
  fun combinedBoldAndListBullet() {
    val f = ActiveFormat.fromJson("""{"bold":true,"list":"bullet"}""")
    assertTrue(f.bold)
    assertEquals("bullet", f.list)
  }
}
