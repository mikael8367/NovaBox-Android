package com.mikael.novabox.model

import com.mikael.novabox.runtime.RuntimeComponentRegistry
import com.mikael.novabox.runtime.RuntimeComponentState
import com.mikael.novabox.model.RuntimeId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeComponentRegistryTest {
    @Test
    fun registryContainsEveryRuntimeExactlyOnce() {
        val registered = RuntimeComponentRegistry.all.map { it.id }
        assertEquals(RuntimeId.entries.toSet(), registered.toSet())
        assertEquals(registered.size, registered.toSet().size)
    }

    @Test
    fun everyComponentHasConcreteFilesToValidate() {
        RuntimeComponentRegistry.all.forEach { component ->
            assertTrue("${component.id} has no file requirements", component.requiredFileGroups.isNotEmpty())
            component.requiredFileGroups.forEach { group ->
                assertFalse("${component.id} has an empty file group", group.isEmpty())
                group.forEach { path ->
                    assertFalse("Absolute paths are forbidden: $path", path.startsWith("/"))
                    assertFalse("Parent traversal is forbidden: $path", path.split('/').contains(".."))
                }
            }
        }
    }

    @Test
    fun turnipRequiresAdrenoAndVulkan() {
        val turnip = RuntimeComponentRegistry.get(RuntimeId.TURNIP)
        assertTrue(turnip.requiresAdreno)
        assertTrue(turnip.requiresVulkan)
    }

    @Test
    fun box64TargetsArm64Host() {
        val box64 = RuntimeComponentRegistry.get(RuntimeId.BOX64)
        assertEquals(setOf("arm64-v8a"), box64.requiredAbi)
    }

    @Test
    fun graphicsTranslationLayersDeclareTheirDependencies() {
        assertTrue(RuntimeComponentRegistry.get(RuntimeId.VIRGL).dependencies.contains(RuntimeId.MESA))
        assertTrue(RuntimeComponentRegistry.get(RuntimeId.ZINK).dependencies.contains(RuntimeId.MESA))
        assertTrue(RuntimeComponentRegistry.get(RuntimeId.TURNIP).dependencies.contains(RuntimeId.MESA))
    }
}
