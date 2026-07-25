package com.apptolast.fledge.domain

import com.apptolast.fledge.domain.model.ChildPin
import com.apptolast.fledge.domain.model.ChildProfile
import com.apptolast.fledge.domain.model.ChildProfileId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ChildProfileModelTest {

    @Test
    fun `AC-04 child profile is not an account`() {
        // Given / When
        val profile = ChildProfile(
            id = ChildProfileId("child-1"),
            displayName = "Lucas",
            age = 9,
            avatarKey = "rocket",
        )

        // Then
        assertEquals("Lucas", profile.displayName)
        assertNull(profile.accountIdentity)
    }

    @Test
    fun `AC-06 child pin accepts four digits only`() {
        // Given / When / Then
        assertEquals("1234", ChildPin("1234").value)
        assertFailsWith<IllegalArgumentException> { ChildPin("123") }
        assertFailsWith<IllegalArgumentException> { ChildPin("12ab") }
    }
}
