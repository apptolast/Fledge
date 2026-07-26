package com.apptolast.fledge.domain

import com.apptolast.fledge.domain.model.ChildPin
import com.apptolast.fledge.domain.model.ChildProfile
import com.apptolast.fledge.domain.model.ChildProfileId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ChildProfileModelTest {

    @Test
    fun `FLE-11 child profile is not an account`() {
        // Given / When
        val profile = ChildProfile(
            id = ChildProfileId("child-1"),
            displayName = "Lucas",
            birthYear = 2017,
            avatarKey = "rocket",
        )

        // Then
        assertEquals("Lucas", profile.displayName)
        assertEquals(2017, profile.birthYear)
    }

    @Test
    fun `AC-06 child pin accepts four digits only`() {
        // Given / When / Then
        assertEquals("1234", ChildPin("1234").value)
        assertFailsWith<IllegalArgumentException> { ChildPin("123") }
        assertFailsWith<IllegalArgumentException> { ChildPin("12ab") }
    }
}
