package com.mikael.novabox.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class QemuCommandBuilderTest {
    @Test fun buildsArm64SystemCommandWithOptionalInitrdAndNetwork() {
        val command = QemuCommandBuilder.build(QemuLaunchSpec(
            executable = "/runtime/qemu-system-aarch64",
            kernel = "/vm/Image",
            initrd = "/vm/initrd.img",
            disk = "/vm/disk.raw",
            ramMb = 768,
            cpuCores = 4,
            append = "console=ttyAMA0 root=/dev/vda",
            networkEnabled = true
        ))
        assertEquals("/runtime/qemu-system-aarch64", command.first())
        assertTrue(command.containsAll(listOf("virt,accel=tcg", "cortex-a53", "768", "4", "-initrd", "/vm/initrd.img")))
        assertTrue(command.contains("console=ttyAMA0 root=/dev/vda"))
        assertTrue(command.contains("virtio-net-device,netdev=net0"))
        assertTrue(command.contains("-no-reboot"))
    }

    @Test fun buildsWithoutInitrdOrNetworkWhenDisabled() {
        val command = QemuCommandBuilder.build(QemuLaunchSpec(
            executable = "qemu", kernel = "Image", initrd = null, disk = "disk.raw",
            ramMb = 512, cpuCores = 1, append = "", networkEnabled = false
        ))
        assertTrue(!command.contains("-initrd"))
        assertTrue(!command.contains("-netdev"))
        assertTrue(!command.contains("-append"))
    }

    @Test fun rejectsInvalidResourceLimitsAndArguments() {
        assertThrows(IllegalArgumentException::class.java) {
            QemuCommandBuilder.build(QemuLaunchSpec("qemu", "Image", null, "disk", 128, 1, "", false))
        }
        assertThrows(IllegalArgumentException::class.java) {
            QemuCommandBuilder.build(QemuLaunchSpec("qemu", "Image", null, "disk", 512, 0, "", false))
        }
        assertThrows(IllegalArgumentException::class.java) {
            QemuCommandBuilder.build(QemuLaunchSpec("qemu", "Image", null, "disk", 512, 1, "bad\u0000arg", false))
        }
    }
}