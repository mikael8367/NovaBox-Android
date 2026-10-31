package com.mikael.novabox.input

enum class MouseButton {
    LEFT,
    RIGHT,
    MIDDLE
}

/**
 * Interface between the touch controller and a future native guest-session backend.
 * The current build provides a visual preview bridge only; it does not inject input
 * into a running Windows or Linux process.
 */
interface DesktopInputBridge {
    fun movePointer(normalizedX: Float, normalizedY: Float)
    fun click(button: MouseButton)
    fun scroll(verticalSteps: Int)
}

class PreviewInputBridge(
    private val onEvent: (String) -> Unit
) : DesktopInputBridge {
    override fun movePointer(normalizedX: Float, normalizedY: Float) {
        onEvent("Mouse: ${(normalizedX * 100).toInt()}%, ${(normalizedY * 100).toInt()}%")
    }

    override fun click(button: MouseButton) {
        onEvent(
            when (button) {
                MouseButton.LEFT -> "Clique esquerdo"
                MouseButton.RIGHT -> "Clique direito"
                MouseButton.MIDDLE -> "Clique do meio"
            }
        )
    }

    override fun scroll(verticalSteps: Int) {
        onEvent(if (verticalSteps < 0) "Rolagem para cima" else "Rolagem para baixo")
    }
}
