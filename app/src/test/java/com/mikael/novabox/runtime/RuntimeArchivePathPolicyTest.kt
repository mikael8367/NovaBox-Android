package com.mikael.novabox.runtime

import java.io.IOException
import java.nio.file.Paths
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class RuntimeArchivePathPolicyTest {
    @Test fun normalizesSafeTarEntries() {
        assertEquals("usr/bin/wine", RuntimeArchivePathPolicy.safeEntryPath("././usr/bin/wine"))
        assertEquals("usr/bin", RuntimeArchivePathPolicy.safeEntryPath("./usr/bin/"))
    }

    @Test fun rejectsAbsoluteTraversalAndWindowsPaths() {
        assertNull(RuntimeArchivePathPolicy.safeEntryPath("/etc/passwd"))
        assertNull(RuntimeArchivePathPolicy.safeEntryPath("../../outside"))
        assertNull(RuntimeArchivePathPolicy.safeEntryPath("C:/Windows/system.ini"))
        assertNull(RuntimeArchivePathPolicy.safeEntryPath("..\\\\outside"))
        assertNull(RuntimeArchivePathPolicy.safeEntryPath(""))
    }

    @Test fun absoluteSymlinkTargetsAreKeptInsideGuestRoot() {
        val root = Paths.get("/tmp/novabox-root").toAbsolutePath()
        val parent = root.resolve("usr/bin")
        assertEquals(root.resolve("usr/lib/libc.so"), RuntimeArchivePathPolicy.resolveLinkTarget(root, parent, "/usr/lib/libc.so"))
    }

    @Test fun rejectsSymlinksThatEscapeGuestRoot() {
        val root = Paths.get("/tmp/novabox-root").toAbsolutePath()
        val parent = root.resolve("usr/bin")
        assertThrows(IOException::class.java) {
            RuntimeArchivePathPolicy.resolveLinkTarget(root, parent, "../../../../../../tmp/escape")
        }
    }
}
