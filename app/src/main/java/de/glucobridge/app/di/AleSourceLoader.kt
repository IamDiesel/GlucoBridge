package de.glucobridge.app.di

import de.glucobridge.core.api.GlucoseSource

/**
 * Laedt die ALE-Quelle NUR, wenn die (git-ignorierte) IP-Zone :data:source-ale im Build ist.
 * Ohne IP-Zone -> null; die App laeuft dann ueber REST (IP-4 / FR-P7).
 * Erwartete Klasse (M3): de.glucobridge.data.ale.AleSourceProvider (object mit create()).
 */
object AleSourceLoader {
    fun tryLoad(): GlucoseSource? = try {
        val cls = Class.forName("de.glucobridge.data.ale.AleSourceProvider")
        val instance = cls.getDeclaredField("INSTANCE").get(null)
        cls.getMethod("create").invoke(instance) as? GlucoseSource
    } catch (_: Throwable) {
        null
    }
}
