package com.mikael.novabox.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class WineCommandBuilderTest {
    @Test fun buildsHostBox64ThenGuestWineCommand() {
        val command = WineCommandBuilder.build(
            "/data/app/files/rootfs/usr/local/bin/box64",
            "/data/app/files/rootfs/opt/wine/bin/wine",
            listOf("/data/app/files/programs/42/setup.program")
        )
        assertEquals(
            listOf(
                "/data/app/files/rootfs/usr/local/bin/box64",
                "/data/app/files/rootfs/opt/wine/bin/wine",
                "/data/app/files/programs/42/setup.program"
            ),
            command
        )
    }

    @Test fun preservesArgumentsContainingSpaces() {
        val command = WineCommandBuilder.build(
            "/runtime/box64",
            "/runtime/wine",
            listOf("/program files/setup.exe", "/quiet mode")
        )
        assertEquals("/program files/setup.exe", command[2])
        assertEquals("/quiet mode", command[3])
    }

    @Test fun rejectsMissingExecutablesAndArguments() {
        assertThrows(IllegalArgumentException::class.java) {
            WineCommandBuilder.build("", "/runtime/wine", listOf("app.exe"))
        }
        assertThrows(IllegalArgumentException::class.java) {
            WineCommandBuilder.build("/runtime/box64", "/runtime/wine", emptyList())
        }
        assertThrows(IllegalArgumentException::class.java) {
            WineCommandBuilder.build("/runtime/box64", "/runtime/wine", listOf(" "))
        }
    }
}
