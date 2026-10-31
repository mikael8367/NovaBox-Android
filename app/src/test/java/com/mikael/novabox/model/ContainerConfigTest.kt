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
        assertTrue(OsCatalog.byFamily(OsFamily.ANDROID).size >= 50)
        assertTrue(OsCatalog.byFamily(OsFamily.OTHER).isNotEmpty())
    }

    @Test
    fun catalogProfileIdsAreUnique() {
        val ids = OsCatalog.profiles.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun windowsAndAndroidEnvironmentsShareInternetByDefault() {
        val windows = ContainerConfig(name = "Windows internet", osProfileId = "windows-10")
        val android = ContainerConfig(name = "Android internet", osProfileId = "android-14")

        assertEquals(NetworkMode.NAT, windows.network)
        assertEquals(NetworkMode.NAT, android.network)
        assertEquals(NetworkMode.DISABLED, windows.copy(network = NetworkMode.DISABLED).network)
        assertEquals(NetworkMode.DISABLED, android.copy(network = NetworkMode.DISABLED).network)
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
    fun everyAndroidGuestProfileUsesQemuVirtualization() {
        val androidProfiles = OsCatalog.byFamily(OsFamily.ANDROID)
        assertTrue("Expected a large Android catalog", androidProfiles.size >= 50)

        androidProfiles.forEach { profile ->
            assertEquals(EngineType.ANDROID_VIRTUALIZATION, profile.engine)
            val config = ContainerConfig(
                name = "Android guest",
                osProfileId = profile.id,
                architecture = when (profile.id) {
                    "android-x86", "android-x86-32" -> CpuArchitecture.X86
                    "android-x86-64", "bliss-os", "prime-os" -> CpuArchitecture.X86_64
                    else -> CpuArchitecture.ARM64
                }
            )
            assertEquals(listOf(RuntimeId.QEMU), RuntimePlanner.requiredFor(config))
            assertEquals(null, config.validate())
        }
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
