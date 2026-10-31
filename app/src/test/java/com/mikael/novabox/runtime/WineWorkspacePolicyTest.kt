package com.mikael.novabox.runtime

import com.mikael.novabox.model.ContainerConfig
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WineWorkspacePolicyTest {
    @Test
    fun acceptsSafeEnvironmentIds() {
        assertTrue(WineWorkspacePolicy.isSafeEnvironmentId("container-123"))
        assertTrue(WineWorkspacePolicy.isSafeEnvironmentId("a_B-9"))
    }

    @Test
    fun rejectsTraversalAndUnexpectedCharacters() {
        assertFalse(WineWorkspacePolicy.isSafeEnvironmentId("../outside"))
        assertFalse(WineWorkspacePolicy.isSafeEnvironmentId("folder/name"))
        assertFalse(WineWorkspacePolicy.isSafeEnvironmentId("id with spaces"))
        assertFalse(WineWorkspacePolicy.isSafeEnvironmentId(""))
        assertFalse(WineWorkspacePolicy.isSafeEnvironmentId("x".repeat(81)))
    }

    @Test
    fun onlyWindowsCompatibilityProfilesCanCreateWineWorkspaces() {
        val windows = ContainerConfig(
            id = "windows-container",
            name = "Windows 10",
            osProfileId = "windows-10"
        )
        val linux = ContainerConfig(
            id = "linux-container",
            name = "Ubuntu",
            osProfileId = "ubuntu-24"
        )

        assertTrue(WineWorkspacePolicy.isWindowsWineProfile(windows))
        assertFalse(WineWorkspacePolicy.isWindowsWineProfile(linux))
    }

    @Test
    fun invalidConfigurationsCannotCreateWineWorkspaces() {
        val invalid = ContainerConfig(
            id = "invalid",
            name = "",
            osProfileId = "windows-10"
        )

        assertFalse(WineWorkspacePolicy.isWindowsWineProfile(invalid))
    }
}
