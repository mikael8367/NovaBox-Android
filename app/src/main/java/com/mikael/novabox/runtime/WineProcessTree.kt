package com.mikael.novabox.runtime

import android.os.Process
import java.io.File

/** Stops Wine helper processes that would otherwise outlive the Box64 root process. */
internal object WineProcessTree {
    internal fun parseChildPids(raw: String): List<Int> = raw.trim()
        .split(Regex("\\s+"))
        .mapNotNull { it.toIntOrNull() }
        .filter { it > 1 }
        .distinct()

    fun descendants(rootPid: Int, procRoot: File = File("/proc")): List<Int> {
        if (rootPid <= 1) return emptyList()
        val visited = linkedSetOf<Int>()
        fun visit(parentPid: Int) {
            val childrenFile = File(procRoot, parentPid.toString() + "/task/" + parentPid + "/children")
            val children = runCatching { parseChildPids(childrenFile.readText()) }.getOrDefault(emptyList())
            children.forEach { childPid ->
                if (childPid != rootPid && visited.add(childPid)) visit(childPid)
            }
        }
        visit(rootPid)
        return visited.toList().asReversed()
    }

    fun terminateDescendants(rootPid: Int) {
        descendants(rootPid).forEach { pid -> runCatching { Process.killProcess(pid) } }
    }
}
