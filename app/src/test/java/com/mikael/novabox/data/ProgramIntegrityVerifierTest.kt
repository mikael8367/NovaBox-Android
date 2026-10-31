package com.mikael.novabox.data

import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Test

class ProgramIntegrityVerifierTest {
    @Test
    fun calculatesStandardSha256Digest() {
        val input = ByteArrayInputStream("abc".toByteArray())
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            ProgramIntegrityVerifier.sha256(input)
        )
    }

    @Test
    fun calculatesDigestForEmptyFile() {
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            ProgramIntegrityVerifier.sha256(ByteArrayInputStream(byteArrayOf()))
        )
    }
}
