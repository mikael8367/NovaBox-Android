package com.mikael.novabox.runtime

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class WineCommandBuilderTest {
    @Test fun launchesMsiThroughWineMsiexec() {
        assertEquals(
            listOf("msiexec", "/i", "/private/installer.msi"),
            WineCommandBuilder.programArguments("installer.msi", "/private/installer.msi")
        )
    }

    @Test fun launchesExeAndComAsDirectWineTargets() {
        assertEquals(
            listOf("/private/app.exe"),
            WineCommandBuilder.programArguments("app.exe", "/private/app.exe")
        )
        assertEquals(
            listOf("/private/tool.com"),
            WineCommandBuilder.programArguments("tool.com", "/private/tool.com")
        )
    }

    @Test fun rejectsUnknownProgramExtensions() {
        assertThrows(IllegalArgumentException::class.java) {
            WineCommandBuilder.programArguments("archive.zip", "/private/archive.zip")
        }
    }

    @Test fun buildsHostBox64ThenGuestWineCommand() {
        val command = WineCommandBuilder.build("/data/app/files/rootfs/usr/local/bin/box64",
            "/data/app/files/rootfs/opt/wine/bin/wine", listOf("/data/app/files/programs/42/setup.program"))
        assertEquals(listOf("/data/app/files/rootfs/usr/local/bin/box64",
            "/data/app/files/rootfs/opt/wine/bin/wine", "/data/app/files/programs/42/setup.program"), command)
    }

    @Test fun preservesArgumentsContainingSpaces() {
        val command = WineCommandBuilder.build("/runtime/box64", "/runtime/wine", listOf("/program files/setup.exe", "/quiet mode"))
        assertEquals("/program files/setup.exe", command[2])
        assertEquals("/quiet mode", command[3])
    }

    @Test fun rejectsMissingExecutablesAndArguments() {
        assertThrows(IllegalArgumentException::class.java) { WineCommandBuilder.build("", "/runtime/wine", listOf("app.exe")) }
        assertThrows(IllegalArgumentException::class.java) { WineCommandBuilder.build("/runtime/box64", "/runtime/wine", emptyList()) }
        assertThrows(IllegalArgumentException::class.java) { WineCommandBuilder.build("/runtime/box64", "/runtime/wine", listOf(" ")) }
    }

    @Test fun buildsWinlatorCompatibleEnvironmentWithIsolatedPrefix() {
        val root = File("/data/user/0/com.mikael.novabox/files/runtimes/rootfs")
        val workspace = File("/data/user/0/com.mikael.novabox/files/wine-workspaces/test")
        val config = ContainerConfig(
            name = "Teste",
            osProfileId = "windows-10",
            esyncEnabled = true,
            fsyncEnabled = false
        )
        val env = WineEnvironmentBuilder.build(root, workspace, config)
        assertEquals(File(workspace, "prefix").absolutePath, env["WINEPREFIX"])
        assertEquals(File(workspace, "home").absolutePath, env["HOME"])
        assertEquals(File(workspace, "tmp").absolutePath, env["TMPDIR"])
        assertEquals("win64", env["WINEARCH"])
        assertEquals("1", env["WINEESYNC"])
        assertFalse(env.containsKey("WINEFSYNC"))
        assertEquals(listOf(File(root, "opt/wine/bin").absolutePath, File(root, "usr/local/bin").absolutePath,
            File(root, "usr/bin").absolutePath).joinToString(File.pathSeparator), env["PATH"])
        assertTrue(env["BOX64_LD_LIBRARY_PATH"]!!.contains("lib/x86_64-linux-gnu"))
        assertEquals("1", env["BOX64_DYNAREC"])
        assertEquals("0", env["BOX64_DYNACACHE"])
        assertFalse(env.containsKey("DISPLAY"))
    }

 
    @Test fun appliesConfiguredWineSynchronizationOptions() {
        val root = createTempDir(prefix = "novabox-rootfs-")
        val workspace = createTempDir(prefix = "novabox-workspace-")
        try {
            val config = ContainerConfig(
                name = "Teste",
                osProfileId = "windows-10",
                esyncEnabled = false,
                fsyncEnabled = true
            )
            val env = WineEnvironmentBuilder.build(root, workspace, config)
            assertFalse(env.containsKey("WINEESYNC"))
            assertEquals("1", env["WINEFSYNC"])
            assertEquals("win64", env["WINEARCH"])
        } finally {
            root.deleteRecursively()
            workspace.deleteRecursively()
        }
    }

    @Test fun includesBox64RcOnlyWhenPresent() {
        val root = createTempDir(prefix = "novabox-rootfs-")
        val workspace = createTempDir(prefix = "novabox-workspace-")
        try {
            val defaultConfig = ContainerConfig(name = "Teste", osProfileId = "windows-10")
            val withoutRc = WineEnvironmentBuilder.build(root, workspace, defaultConfig)
            assertFalse(withoutRc.containsKey("BOX64_RCFILE"))
            val rc = File(root, "etc/config.box64rc")
            rc.parentFile.mkdirs()
            rc.writeText("# test")
            val withRc = WineEnvironmentBuilder.build(root, workspace, defaultConfig)
            assertEquals(rc.absolutePath, withRc["BOX64_RCFILE"])
        } finally {
            root.deleteRecursively()
            workspace.deleteRecursively()
        }
    }
}