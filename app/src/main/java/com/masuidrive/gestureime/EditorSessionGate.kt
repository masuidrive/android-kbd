package com.masuidrive.gestureime

internal class EditorSessionGate {
    private var generation = 0L

    fun capture(): Long = generation

    fun advance() {
        generation++
    }

    fun isCurrent(token: Long): Boolean = token == generation

    inline fun runIfCurrent(token: Long, block: () -> Unit): Boolean {
        if (!isCurrent(token)) return false
        block()
        return true
    }
}
