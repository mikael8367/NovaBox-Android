package com.mikael.novabox.runtime

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class WineProcessTreeTest {
    @Test fun parsesChildPidListAndIgnoresInvalidValues() {
        assertEquals(listOf(21, 34), WineProcessTree.parseChildPids("21 0 nope 34 21"))
    }

    @Test fun readsDescendantsRecursivelyAndReturnsLeavesFirst() {
        val proc = createTempDir(prefix = "novabox-proc-test")
        try {
            fun writeChildren(pid: Int, value: String) {
                val task = File(proc, pid.toString() + "/task/" + pid)
                assert(task.mkdirs())
                File(task, "children").writeText(value)
            }
            writeChildren(10, "11 12")
            writeChildren(11, "13")
            writeChildren(12, "")
            writeChildren(13, "")
            assertEquals(listOf(12, 13, 11), WineProcessTree.descendants(10, proc))
        } finally { proc.deleteRecursively() }
    }

    @Test fun rejectsInvalidRootPid() {
        assertEquals(emptyList<Int>(), WineProcessTree.descendants(1, File("/does-not-exist")))
    }
}
