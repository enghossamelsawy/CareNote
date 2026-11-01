package com.hossam.carenote.core.designsystem.component

import org.junit.Assert.assertEquals
import org.junit.Test

class InitialsTest {
    @Test
    fun `given a first and last name, when initials are made, then both first letters are used`() {
        // When
        val initials = initialsOf("Anna Becker")

        // Then
        assertEquals("AB", initials)
    }

    @Test
    fun `given a middle name, when initials are made, then only the first and last name count`() {
        // When
        val initials = initialsOf("Anna Maria Becker")

        // Then
        assertEquals("AB", initials)
    }

    @Test
    fun `given a single name, when initials are made, then one letter is used`() {
        // When
        val initials = initialsOf("Karl")

        // Then
        assertEquals("K", initials)
    }

    @Test
    fun `given extra spaces and lower case, when initials are made, then they are trimmed and upper case`() {
        // When
        val initials = initialsOf("  greta   hoffmann ")

        // Then
        assertEquals("GH", initials)
    }

    @Test
    fun `given an empty name, when initials are made, then there are none`() {
        // When
        val initials = initialsOf("   ")

        // Then
        assertEquals("", initials)
    }
}
