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

class LinuxCommandPolicyTest {
    @Test fun rejectsBlankCommands() {
        assertEquals("Digite um comando.", LinuxCommandPolicy.validate("  "))
    }
    @Test fun rejectsOversizedCommands() {
        assertEquals("O comando excede o limite de 4.096 caracteres.", LinuxCommandPolicy.validate("x".repeat(4097)))
    }
    @Test fun rejectsNullBytes() {
        assertEquals("O comando contém um caractere inválido.", LinuxCommandPolicy.validate("echo\u0000x"))
    }
    @Test fun acceptsNormalShellCommand() {
        assertNull(LinuxCommandPolicy.validate("uname -a"))
    }
    @Test fun commandArgumentsKeepEachArgumentSeparate() {
        val args = LinuxCommandPolicy.buildArguments("/private/proot", "/private/rootfs", "/private/shared", "printf '%s' 'hello world'")
        assertEquals("/private/proot", args.first())
        assertTrue(args.containsAll(listOf("-r", "/private/rootfs", "-w", "/root", "/bin/sh", "-lc")))
        assertEquals("printf '%s' 'hello world'", args.last())
        assertFalse(args.any { it == "hello world" })
    }
}
