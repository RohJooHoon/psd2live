package io.github.psd2live.tools

import io.github.psd2live.core.CharacterAnalyzer
import io.github.psd2live.core.PSD2LivePipeline
import kotlin.test.Test

/** Temporary probe: the mouth at small openings, per form. */
class MouthSmallOpenTool {
	@Test fun render() {
		requireTools()
		val sample = Sample.fromEnvironment()
		val model = PSD2LivePipeline().buildPreview(sample.path)
		val m = model.analysis.layers.filter { it.semantic.tag in CharacterAnalyzer.MOUTH_TAGS && it.opaquePixels > 0 }
			.map { it.bounds }.reduce { a, b -> a.union(b) }
		val pad = m.width * 0.25f
		val rect = io.github.psd2live.core.Bounds(m.left - pad, m.top - pad, m.right + pad, m.bottom + pad)
		val renderer = Renderer(model, 360)
		val frames = listOf(-1f, 0f).flatMap { form -> listOf(0f, 0.04f, 0.08f, 0.12f, 0.18f, 0.25f).map { open ->
			"form $form open $open" to renderer.render(mapOf("ParamMouthForm" to form, "ParamMouthOpenY" to open), rect)
		} }
		sheet(frames, output("small-open/${sample.name}").resolve("poses.png"), columns = 6)
	}
}
