package com.mikael.novabox.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContainerConfigTest {
    @Test
    fun catalogContainsEachRequestedSystemFamily() {
        assertTrue(OsCatalog.byFamily(OsFamily.WINDOWS).size >= 10)
        assertTrue(OsCatalog.byFamily(OsFamily.LINUX).size >= 10)
        assertTrue(OsCatalog.byFamily(OsFamily.BSD).isNotEmpty())
        assertTrue(OsCatalog.byFamily(OsFamily.OTHER).isNotEmpty())
    }

    @Test
    fun catalogProfileIdsAreUnique() {
        val ids = OsCatalog.profiles.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun defaultWindowsProfileNeedsWineAndBox64() {
        val config = ContainerConfig(
            name = "Windows teste",
            osProfileId = "windows-10",
            architecture = CpuArchitecture.X86_64
        )
        val required = RuntimePlanner.requiredFor(config)
        assertTrue(required.contains(RuntimeId.WINE))
        assertTrue(required.contains(RuntimeId.BOX64))
    }

    @Test
    fun windowsDxvkOptionsAreAddedOnlyWhenSelected() {
        val base = ContainerConfig(name = "Windows", osProfileId = "windows-10")
        val configured = base.copy(dxvkEnabled = true, vkd3dEnabled = true)
        val baseRequired = RuntimePlanner.requiredFor(base)
        val configuredRequired = RuntimePlanner.requiredFor(configured)

        assertFalse(baseRequired.contains(RuntimeId.DXVK))
        assertFalse(baseRequired.contains(RuntimeId.VKD3D))
        assertTrue(configuredRequired.contains(RuntimeId.DXVK))
        assertTrue(configuredRequired.contains(RuntimeId.VKD3D))
    }

    @Test
    fun nativeArmLinuxProfileDoesNotRequireX86Translator() {
        val config = ContainerConfig(
            name = "Debian ARM",
            osProfileId = "debian-12",
            architecture = CpuArchitecture.ARM64
        )
        val required = RuntimePlanner.requiredFor(config)
        assertTrue(required.contains(RuntimeId.PROOT))
        assertFalse(required.contains(RuntimeId.BOX64))
        assertFalse(required.contains(RuntimeId.BOX86))
    }

    @Test
    fun fullSystemProfilesRequireQemu() {
        val config = ContainerConfig(
            name = "FreeBSD",
            osProfileId = "freebsd",
            architecture = CpuArchitecture.ARM64
        )
        assertTrue(RuntimePlanner.requiredFor(config).contains(RuntimeId.QEMU))
    }

    @Test
    fun freedosUsesDosbox() {
        val config = ContainerConfig(name = "DOS", osProfileId = "freedos")
        assertEquals(listOf(RuntimeId.DOSBOX), RuntimePlanner.requiredFor(config))
    }

    @Test
    fun configurationValidationRejectsInvalidValues() {
        val valid = ContainerConfig(name = "Windows 10", osProfileId = "windows-10")
        assertEquals(null, valid.validate())
        assertNotNull(valid.copy(name = " ").validate())
        assertNotNull(valid.copy(ramMb = 128).validate())
        assertNotNull(valid.copy(cpuCores = 0).validate())
        assertNotNull(valid.copy(diskGb = 1).validate())
        assertNotNull(valid.copy(dpi = 300).validate())
        assertNotNull(valid.copy(fpsLimit = 75).validate())
        assertNotNull(valid.copy(mouseSensitivity = 11).validate())
        assertNotNull(valid.copy(graphicsMemoryMb = 4096).validate())
    }
}
