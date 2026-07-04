package com.recipesaver.app.ui.screens

import org.junit.Assert.assertEquals
import org.junit.Test

/** Unit tests for [toLines], which turns free-text ingredient/step input into a clean list. */
class TextParsingTest {
    @Test
    fun `splits on newlines`() {
        assertEquals(listOf("a", "b", "c"), "a\nb\nc".toLines())
    }

    @Test
    fun `trims surrounding whitespace on each line`() {
        assertEquals(listOf("a", "b"), "  a  \n\tb\t".toLines())
    }

    @Test
    fun `drops blank and whitespace-only lines`() {
        assertEquals(listOf("a", "b"), "a\n\n   \nb\n".toLines())
    }

    @Test
    fun `empty input yields empty list`() {
        assertEquals(emptyList<String>(), "".toLines())
        assertEquals(emptyList<String>(), "   \n  ".toLines())
    }
}
