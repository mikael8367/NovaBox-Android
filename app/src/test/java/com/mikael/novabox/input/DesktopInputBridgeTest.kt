package com.mikael.novabox.input

import org.junit.Assert.assertEquals
import org.junit.Test

class DesktopInputBridgeTest {
    @Test
    fun pointerMovementIsReportedAsNormalizedPosition() {
        val events = mutableListOf<String>()
        val bridge = PreviewInputBridge { events.add(it) }

        bridge.movePointer(0.25f, 0.50f)

        assertEquals(listOf("Mouse: 25%, 50%"), events)
    }

    @Test
    fun mouseButtonsProduceDistinctActions() {
        val events = mutableListOf<String>()
        val bridge = PreviewInputBridge { events.add(it) }

        bridge.click(MouseButton.LEFT)
        bridge.click(MouseButton.RIGHT)
        bridge.click(MouseButton.MIDDLE)

        assertEquals(
            listOf("Clique esquerdo", "Clique direito", "Clique do meio"),
            events
        )
    }

    @Test
    fun scrollDirectionIsPreserved() {
        val events = mutableListOf<String>()
        val bridge = PreviewInputBridge { events.add(it) }

        bridge.scroll(-1)
        bridge.scroll(1)

        assertEquals(listOf("Rolagem para cima", "Rolagem para baixo"), events)
    }
}
