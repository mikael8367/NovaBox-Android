package com.mikael.novabox.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProotPackageManifestTest {
    @Test fun pinnedArchiveUsesGitBlobObjectHash() {
        val sample = "hello\n".toByteArray()
        assertEquals("ce013625030ba8dba906f756967f9e9ca394464a", ProotPackageManifest.gitBlobSha1(sample))
    }

    @Test fun upstreamArchiveIsPinnedToAnImmutableCommit() {
        assertTrue(ProotPackageManifest.URL.contains(ProotPackageManifest.UPSTREAM_COMMIT))
        assertEquals(179_448L, ProotPackageManifest.SIZE)
        assertEquals(40, ProotPackageManifest.GIT_BLOB_SHA1.length)
    }
}

