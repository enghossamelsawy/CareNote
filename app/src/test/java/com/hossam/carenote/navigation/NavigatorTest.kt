package com.hossam.carenote.navigation

import androidx.navigation3.runtime.NavKey
import com.hossam.carenote.feature.record.navigation.RecordKey
import com.hossam.carenote.feature.resident.navigation.ResidentKey
import com.hossam.carenote.feature.residents.navigation.ResidentsKey
import org.junit.Assert.assertEquals
import org.junit.Test

class NavigatorTest {
    private val backStack = mutableListOf<NavKey>(ResidentsKey)
    private val navigator = Navigator(backStack)

    @Test
    fun `given the residents list on top, when a resident is tapped, then the resident opens`() {
        // When
        navigator.navigate(from = ResidentsKey, to = ResidentKey("1"))

        // Then
        assertEquals(listOf(ResidentsKey, ResidentKey("1")), backStack)
    }

    @Test
    fun `given a resident was just opened, when the same resident is tapped again, then it opens only once`() {
        // Given
        navigator.navigate(from = ResidentsKey, to = ResidentKey("1"))

        // When
        navigator.navigate(from = ResidentsKey, to = ResidentKey("1"))

        // Then
        assertEquals(listOf(ResidentsKey, ResidentKey("1")), backStack)
    }

    @Test
    fun `given a resident was just opened, when another resident is tapped quickly, then it is ignored`() {
        // Given
        navigator.navigate(from = ResidentsKey, to = ResidentKey("1"))

        // When
        navigator.navigate(from = ResidentsKey, to = ResidentKey("2"))

        // Then
        assertEquals(listOf(ResidentsKey, ResidentKey("1")), backStack)
    }

    @Test
    fun `given record was just opened, when record is tapped again, then it opens only once`() {
        // Given
        navigator.navigate(from = ResidentsKey, to = ResidentKey("1"))
        navigator.navigate(from = ResidentKey("1"), to = RecordKey("1"))

        // When
        navigator.navigate(from = ResidentKey("1"), to = RecordKey("1"))

        // Then
        assertEquals(listOf(ResidentsKey, ResidentKey("1"), RecordKey("1")), backStack)
    }

    @Test
    fun `given a resident is open, when back is pressed, then the residents list shows again`() {
        // Given
        navigator.navigate(from = ResidentsKey, to = ResidentKey("1"))

        // When
        navigator.goBack()

        // Then
        assertEquals(listOf(ResidentsKey), backStack)
    }

    @Test
    fun `given only the residents list, when back is pressed, then the list stays`() {
        // When
        navigator.goBack()

        // Then
        assertEquals(listOf(ResidentsKey), backStack)
    }
}
