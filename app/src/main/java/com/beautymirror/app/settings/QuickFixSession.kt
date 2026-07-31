package com.beautymirror.app.settings

/**
 * Stacks one-tap corrections on a shared baseline so undoing one fix does not wipe the others.
 */
class QuickFixSession {
    private var baseline: BeautySettings? = null
    private val applies = linkedMapOf<String, (BeautySettings) -> BeautySettings>()

    val activeIds: Set<String>
        get() = applies.keys.toSet()

    fun isActive(id: String): Boolean = id in applies

    fun toggle(
        id: String,
        current: BeautySettings,
        apply: (BeautySettings) -> BeautySettings,
    ): BeautySettings {
        if (id in applies) {
            applies.remove(id)
            if (applies.isEmpty()) {
                val restored = baseline ?: current
                baseline = null
                return restored
            }
            return fold(requireNotNull(baseline))
        }
        if (baseline == null) baseline = current
        applies[id] = apply
        return fold(requireNotNull(baseline))
    }

    /** Clears stacked fixes. Returns baseline when one existed. */
    fun clear(): BeautySettings? {
        val restored = baseline
        baseline = null
        applies.clear()
        return restored
    }

    private fun fold(base: BeautySettings): BeautySettings {
        var next = base
        for (fn in applies.values) {
            next = fn(next)
        }
        return next
    }
}
