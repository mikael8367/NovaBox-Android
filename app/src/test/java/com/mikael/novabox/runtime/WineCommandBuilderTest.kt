package com.mikael.novabox.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WineCommandBuilderTest {
    @Test
    fun buildsRootfsCommandWithBox64AndWorkspaceBindings() {
        val command = WineCommandBuilder.build(
            prootExecutable = "/data/user/0/app/files/runtimes/proot/bin/proot",
            rootfsDirectory = "/data/user/0/app/files/runtimes/rootfs",
            workspaceDirectory = "/data/user/0/app/files/wine-workspaces/win-1",
            sharedProgramsDirectory = "/data/user/0/app/files/programs/win-1",
            guestCommand = listOf("/usr/bin/wine", "/programs/setup program.exe")
        )

        assertEquals("/data/user/0/app/files/runtimes/proot/bin/proot", command.first())
        assertTrue(command.contains("-r"))
        assertTrue(command.contains("/data/user/0/app/files/runtimes/rootfs"))
        assertTrue(command.contains("/data/user/0/app/files/wine-workspaces/win-1:/workspace"))
        assertTrue(command.contains("/data/user/0/app/files/programs/win-1:/programs"))
        assertEquals("/usr/bin/box64", command[command.indexOf("-w") + 2])
        assertEquals("/usr/bin/wine", command[command.lastIndexOf("/usr/bin/wine")])
        assertEquals("/programs/setup program.exe", command.last())
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsEmptyGuestCommand() {
        WineCommandBuilder.build("/proot", "/rootfs", "/workspace", "/programs", emptyList())
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsBlankGuestArguments() {
        WineCommandBuilder.build("/proot", "/rootfs", "/workspace", "/programs", listOf("/usr/bin/wine", " "))
    }
}
