package com.mikael.novabox.runtime

import com.mikael.novabox.model.RuntimeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimePackageCatalogTest {
    @Test
    fun onlyPackagesWithCompleteIntegrityMetadataAreInstallable() {
        val packages = RuntimePackageCatalog.verifiedPackages()
        assertEquals(setOf(RuntimeId.DXVK), packages.map { it.runtimeId }.toSet())
        packages.forEach { spec ->
            assertTrue(spec.downloadUrl.startsWith("https://"))
            assertTrue(spec.sha256.matches(Regex("^[a-fA-F0-9]{64}$")))
            assertTrue(spec.expectedSizeBytes > 0L)
            assertTrue(spec.allowedArchiveFiles.isNotEmpty())
        }
    }

    @Test
    fun dxvkPackageUsesPinnedReleaseAndExpectedDlls() {
        val spec = RuntimePackageCatalog.packageFor(RuntimeId.DXVK)
        assertNotNull(spec)
        assertEquals("3.1.1", spec!!.version)
        assertEquals(18_041_512L, spec.expectedSizeBytes)
        assertEquals(
            "40565b4a724aadc4433fa4e010b4b23916d9b1f1baeee64e17186db94f54e608",
            spec.sha256
        )
        assertEquals(8, spec.requiredArchiveFiles.size)
        assertTrue(spec.allowedArchiveFiles.contains("LICENSE"))
        assertTrue(spec.allowedArchiveFiles.contains("x64/dxgi.dll"))
        assertTrue(spec.allowedArchiveFiles.contains("x32/d3d9.dll"))
    }

    @Test
    fun unverifiedEnginesDoNotReceiveDownloadPackages() {
        RuntimeId.entries.filter { it != RuntimeId.DXVK }.forEach { id ->
            assertTrue("$id unexpectedly has an unverified installer", RuntimePackageCatalog.packageFor(id) == null)
            assertTrue(RuntimePackageCatalog.unavailableReason(id).isNotBlank())
        }
    }
}
