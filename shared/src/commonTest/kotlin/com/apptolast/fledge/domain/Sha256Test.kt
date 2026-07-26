package com.apptolast.fledge.domain

import com.apptolast.fledge.domain.security.Sha256
import kotlin.test.Test
import kotlin.test.assertEquals

class Sha256Test {

    @Test
    fun `FLE-11 sha256 uses stable hex digest`() {
        // Given / When / Then
        assertEquals(
            "03ac674216f3e15c761ee1a5e255f067953623c8b388b4459e13f978d7c846f4",
            Sha256.hashHex("1234"),
        )
    }
}
