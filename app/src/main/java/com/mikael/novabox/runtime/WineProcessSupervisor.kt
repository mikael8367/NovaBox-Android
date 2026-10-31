package com.mikael.novabox.runtime

import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/** Tracks one foreground Wine process per environment. */
object WineProcessSupervisor {
    private val processes = ConcurrentHashMap<String, Process>()

    @Throws(IOException::class)
    fun start(environmentId: String, create: () -> Process): Process {
        require(environmentId.matches(Regex("^[A-Za-z0-9_-]{1,80}$"))) {
            "Identificador do ambiente inválido."
        }
        synchronized(processes) {
            val previous = processes[environmentId]
            if (previous?.isAlive == true) {
                throw IOException("Já existe um processo Wine ativo neste ambiente.")
            }
            if (previous != null) processes.remove(environmentId, previous)
            val process = create()
            processes[environmentId] = process
            Thread({
                try {
                    process.waitFor()
                } catch (_: InterruptedException) {
                    Thread.currentThread().interrupt()
                } finally {
                    processes.remove(environmentId, process)
                }
            }, "NovaBox-Wine-" + environmentId.take(24)).apply {
                isDaemon = true
                start()
            }
            return process
        }
    }

    fun isRunning(environmentId: String): Boolean =
        processes[environmentId]?.isAlive == true

    fun activeEnvironmentIds(): Set<String> =
        processes.entries.filter { it.value.isAlive }.map { it.key }.toSet()

    fun stop(environmentId: String, timeoutMillis: Long = 1_500L): Boolean {
        require(timeoutMillis >= 0L) { "Tempo limite inválido." }
        val process = processes[environmentId] ?: return false
        if (!process.isAlive) {
            processes.remove(environmentId, process)
            return true
        }
        process.destroy()
        try {
            if (!process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)) {
                process.destroyForcibly()
                process.waitFor(timeoutMillis.coerceAtLeast(1L), TimeUnit.MILLISECONDS)
            }
        } catch (_: InterruptedException) {
            process.destroyForcibly()
            Thread.currentThread().interrupt()
        } finally {
            if (!process.isAlive) processes.remove(environmentId, process)
        }
        return !process.isAlive
    }

    fun stopAll(timeoutMillis: Long = 1_500L): Set<String> {
        require(timeoutMillis >= 0L) { "Tempo limite inválido." }
        return activeEnvironmentIds().filterNot { stop(it, timeoutMillis) }.toSet()
    }
}