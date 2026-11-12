package io.github.psd2live.core

/**
 * Whether the gloom mood fades a blush as it does a highlight (`PSD2LIVE_GLOOM_BLUSH=1`), so a greyed face keeps no rosy
 * cheeks. Off by default: the gloom stage's output is locked to its released versions.
 */
internal object MoodBlush {
	val fades: Boolean get() = System.getenv("PSD2LIVE_GLOOM_BLUSH") == "1"
}
