package io.github.psd2live.core.fit

import io.github.psd2live.core.LayerClassifier
import io.github.psd2live.core.PartRole
import io.github.psd2live.core.PartRoles
import io.github.psd2live.core.SemanticTag
import org.umamo.format.art.LayerBlend
import org.umamo.format.art.LayerBounds
import org.umamo.format.art.LayerId
import org.umamo.format.art.LayerRaster
import org.umamo.format.art.SourceArt
import org.umamo.format.art.SourceLayer
import java.awt.RenderingHints
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * A mouth drawn shut with its lip lines and nothing behind them - no opening, teeth or tongue, as a quick Live2D PSD
 * often has - gets the opening drawn for it: a dark mouth shape of a usual open mouth's proportions, from the shut lines'
 * corners, added to the art as an ordinary layer (`mouth inside (generated)`) just behind the lines. The fitted mouth
 * then opens the lines onto it as it does a drawn inside; the layer can be repainted like any other.
 */
object MouthInsideSynthesis {
	/** How far the opening reaches above and below the shut line at its middle, in mouth widths. */
	private const val UPPER_REACH = 0.20f
	private const val LOWER_REACH = 0.42f

	/**
	 * How much wider than the shut line the opening is drawn: a mouth opens past where its shut line ends (the corners
	 * spread as the lips part), so an inside cut to the line's length opens too small and only up and down.
	 */
	private const val OPEN_SPREAD = 1.2f

	private val MOUTHS = setOf(SemanticTag.MOUTH, SemanticTag.MOUTH_OPEN)

	/** The least of a split line each lip keeps, in canvas pixels. */
	private const val MIN_LINE = 2.5f

	/** The upper lip's share of a split line's weight: a little more than half, as drawn open mouths often are. */
	private const val UPPER_SHARE = 0.6f

	/**
	 * [art] with a generated mouth inside when its mouth is drawn shut over nothing - first splitting a mouth drawn as one
	 * shut line into upper and lower lip lines with skin about them ([splitLine]); [art] itself otherwise.
	 */
	fun augment(art: SourceArt): SourceArt = addInside(if (enabled) splitLine(art) else art)

	/**
	 * Splitting a single shut line and fitting a mouth kit change what a PSD generates, so they are opt in for now:
	 * `PSD2LIVE_MOUTH_SYNTHESIS=1` (or an explicit `PSD2LIVE_MOUTH_KIT`). Drawing an inside for lines drawn shut over
	 * nothing is always on - those mouths did not open at all before.
	 */
	private val enabled: Boolean get() = System.getenv("PSD2LIVE_MOUTH_SYNTHESIS") == "1"

	private val LIP_ROLES = setOf(PartRole.UPPER_LINE, PartRole.LOWER_LINE, PartRole.UPPER_LIP, PartRole.LOWER_LIP)

	/**
	 * A mouth drawn as a single shut line - one thin mouth layer, no lips, lines or inside - as the hand rigger's two lip
	 * lines with their skin: the line once with a band of the face's skin above it (`upper lip line`, keeping the
	 * layer's id) and once with a band below it (`lower lip line`), the bands fading into the face and narrowing to the
	 * corners. Shut, the two sit exactly on the drawing; open, each band follows its lip and covers what is behind it.
	 */
	private fun splitLine(art: SourceArt): SourceArt {
		val named = art.layers.map { layer ->
			val semantic = LayerClassifier.classify(layer.name)
			Triple(layer, semantic.tag, PartRoles.fromName(layer.name, layer.groupPath, semantic.tag))
		}
		if (named.any { (_, tag, role) -> tag in MOUTHS && role in LIP_ROLES }) return art
		val mouths = named.filter { (layer, tag, role) -> tag in MOUTHS && role == null && !layer.clipped && opaque(layer) }
		val line = mouths.singleOrNull()?.first ?: return art
		val centre = centreline(line) ?: return art
		val left = centre.first().x; val right = centre.last().x; val width = right - left
		val thickness = thickness(line)
		// A line, not a mouth drawn open: thin against its width.
		if (width < 8f || thickness > 0.25f * width) return art
		val skin = skinColor(named.filter { it.second == SemanticTag.FACE && it.third == null }.map { it.first },
			left - width, centre.minOf { it.y } - width, right + width, centre.maxOf { it.y } + width) ?: return art
		val band = 0.25f * width + thickness
		// Each copy keeps its own half of the line - the upper the top of the stroke, the lower its bottom - so a thick line
		// parts into two lines of its weight between them, and shut the halves make up exactly the drawn line. A thin line
		// keeps at least [MIN_LINE] in each copy, overlapping in the middle.
		val upper = lipLine(line, centre, thickness, band, skin, above = true,
			LayerId(line.id.raw), "upper lip line") ?: return art
		val lower = lipLine(line, centre, thickness, band, skin, above = false,
			LayerId("${line.id.raw}::lower-lip-line"), "lower lip line") ?: return art
		val layers = art.layers.toMutableList().apply { val at = indexOf(line); set(at, upper); add(at + 1, lower) }
		return object : SourceArt by art { override val layers: List<SourceLayer> = layers }
	}

	/** The line's middle in each canvas column where it has opaque pixels. */
	private fun centreline(layer: SourceLayer): List<P>? {
		val raster = layer.raster; val b = layer.bounds
		val w = raster.width; val h = raster.height
		if (w <= 0 || h <= 0) return null
		val sx = b.width / w.toFloat(); val sy = b.height / h.toFloat()
		val out = ArrayList<P>()
		for (x in 0 until w) {
			var sum = 0f; var weight = 0f
			for (y in 0 until h) { val a = alpha(raster, y * w + x) / 255f; sum += a * y; weight += a }
			if (weight > 0.5f) out += P(b.left + (x + 0.5f) * sx, b.top + (sum / weight + 0.5f) * sy)
		}
		return out.takeIf { it.size >= 4 }
	}

	/** The line's mean thickness across its columns, in canvas pixels. */
	private fun thickness(layer: SourceLayer): Float {
		val raster = layer.raster; val w = raster.width; val h = raster.height
		var sum = 0f; var columns = 0
		for (x in 0 until w) {
			var weight = 0f
			for (y in 0 until h) weight += alpha(raster, y * w + x) / 255f
			if (weight > 0.5f) { sum += weight; columns++ }
		}
		return if (columns == 0) 0f else sum / columns * (layer.bounds.height / h.toFloat())
	}

	/** The face's skin about the mouth: the median of the face layers' opaque pixels in the box. */
	private fun skinColor(faces: List<SourceLayer>, x0: Float, y0: Float, x1: Float, y1: Float): Triple<Int, Int, Int>? {
		val rs = ArrayList<Int>(); val gs = ArrayList<Int>(); val bs = ArrayList<Int>()
		for (face in faces) {
			val raster = face.raster; val b = face.bounds
			val sx = b.width / raster.width.toFloat(); val sy = b.height / raster.height.toFloat()
			for (y in 0 until raster.height) {
				val cy = b.top + (y + 0.5f) * sy
				if (cy < y0 || cy > y1) continue
				for (x in 0 until raster.width) {
					val cx = b.left + (x + 0.5f) * sx
					if (cx < x0 || cx > x1) continue
					val p = y * raster.width + x
					if (alpha(raster, p) < 250) continue
					rs += raster.rgba[p * 4].toInt() and 0xff; gs += raster.rgba[p * 4 + 1].toInt() and 0xff; bs += raster.rgba[p * 4 + 2].toInt() and 0xff
				}
			}
		}
		if (rs.size < 16) return null
		fun median(v: ArrayList<Int>) = v.sorted()[v.size / 2]
		return Triple(median(rs), median(gs), median(bs))
	}

	/**
	 * [line] drawn over a band of [skin] on one side of it: from its middle to [band] beyond, narrowing to nothing at the
	 * corners and fading over its outer half into the face.
	 */
	private fun lipLine(line: SourceLayer, centre: List<P>, thickness: Float, band: Float, skin: Triple<Int, Int, Int>,
	                    above: Boolean, id: LayerId, name: String): SourceLayer? {
		val lb = line.bounds
		val left = centre.first().x; val width = centre.last().x - left
		val x0 = lb.left - 2; val x1 = lb.left + lb.width + 2
		val y0 = floor(min(lb.top.toFloat(), centre.minOf { it.y } - band - thickness)).toInt() - 2
		val y1 = ceil(max((lb.top + lb.height).toFloat(), centre.maxOf { it.y } + band + thickness)).toInt() + 2
		val w = x1 - x0; val h = y1 - y0
		if (w <= 0 || h <= 0) return null
		val rgba = ByteArray(w * h * 4)
		for (x in 0 until w) {
			val cx = x0 + x + 0.5f
			val t = ((cx - left) / width.coerceAtLeast(1f)).coerceIn(0f, 1f)
			val reach = band * sin(PI.toFloat() * t).coerceAtLeast(0f).pow(0.5f)
			if (reach <= 0.5f) continue
			val mid = at(centre, cx)
			for (y in 0 until h) {
				val cy = y0 + y + 0.5f
				val d = if (above) mid - cy else cy - mid  // distance out from the line's middle
				// From the line's middle outward only: the inner half of the line stays over nothing, so no skin shows
				// along the opening's edge when the lips part.
				if (d < 0f || d > reach) continue
				val fade = ((reach - d) / (0.5f * reach)).coerceIn(0f, 1f)
				val i = (y * w + x) * 4
				rgba[i] = skin.first.toByte(); rgba[i + 1] = skin.second.toByte(); rgba[i + 2] = skin.third.toByte()
				rgba[i + 3] = (255 * fade).toInt().toByte()
			}
		}
		// The line itself over the band, straight alpha "over": this copy's half of it, softened over a pixel at the cut.
		val raster = line.raster
		val sx = lb.width / raster.width.toFloat(); val sy = lb.height / raster.height.toFloat()
		// The cut runs [UPPER_SHARE] of the way down the line; each copy keeps at least [MIN_LINE] of it.
		val cut = (UPPER_SHARE - 0.5f) * thickness
		val share = if (above) UPPER_SHARE else 1f - UPPER_SHARE
		val overlap = max(0.75f, MIN_LINE - share * thickness)
		for (y in 0 until raster.height) for (x in 0 until raster.width) {
			val p = y * raster.width + x
			val cx = lb.left + (x + 0.5f) * sx; val cy = lb.top + (y + 0.5f) * sy
			val outward = if (above) at(centre, cx) + cut - cy else cy - at(centre, cx) - cut  // distance out from the cut
			val keep = (outward + overlap + 0.5f).coerceIn(0f, 1f)
			val a = alpha(raster, p) / 255f * keep
			if (a <= 0f) continue
			val tx = cx.toInt() - x0; val ty = cy.toInt() - y0
			if (tx !in 0 until w || ty !in 0 until h) continue
			val i = (ty * w + tx) * 4
			val under = (rgba[i + 3].toInt() and 0xff) / 255f
			val out = a + under * (1f - a)
			for (c in 0..2) {
				val src = (raster.rgba[p * 4 + c].toInt() and 0xff); val dst = (rgba[i + c].toInt() and 0xff)
				rgba[i + c] = ((src * a + dst * under * (1f - a)) / out.coerceAtLeast(1e-4f)).toInt().coerceIn(0, 255).toByte()
			}
			rgba[i + 3] = (255 * out).toInt().coerceIn(0, 255).toByte()
		}
		return Generated(id, name, line.groupPath, line.order, LayerBounds(x0, y0, w, h), LayerRaster(w, h, rgba))
	}

	private fun addInside(art: SourceArt): SourceArt {
		val named = art.layers.map { layer ->
			val semantic = LayerClassifier.classify(layer.name)
			Triple(layer, semantic.tag, PartRoles.fromName(layer.name, layer.groupPath, semantic.tag))
		}
		// An opening or a whole mouth is drawn already.
		if (named.any { (layer, tag, role) -> tag in MOUTHS && role == null && !layer.clipped && opaque(layer) }) return art
		val upper = named.filter { it.second in MOUTHS && it.third == PartRole.UPPER_LINE }.maxByOrNull { area(it.first) }?.first ?: return art
		val lower = named.filter { it.second in MOUTHS && it.third == PartRole.LOWER_LINE }.maxByOrNull { area(it.first) }?.first ?: return art
		val up = stroke(upper) ?: return art
		val lo = stroke(lower) ?: return art
		val left = min(up.first().x, lo.first().x); val right = max(up.last().x, lo.last().x)
		val width = right - left
		if (width < 8f) return art
		// Only lines that meet - a mouth drawn shut - get an inside; lines drawn apart already show their opening.
		val apart = (0..40).maxOf { k -> val x = left + width * (0.25f + 0.5f * k / 40f); abs(at(up, x) - at(lo, x)) }
		// Halves of a split thick line lie apart by half its weight: still drawn shut.
		if (apart > 0.12f * width) return art
		// The open mouth is built about the line between the corners, not the shut line itself: a shut line's own bend (a
		// pout's arch, a smile's dip) is where the lips meet, and would otherwise put a notch in the open lip.
		val mid = chord(left, width) { x -> 0.5f * (at(up, x) + at(lo, x)) }
		val openLeft = left - 0.5f * (OPEN_SPREAD - 1f) * width; val openWidth = OPEN_SPREAD * width
		// Behind both lines: they take the order of the line further back (orders grow away from the viewer) and come
		// right after that line in the list, so a stable sort by order puts them behind the line they tie with.
		val back = if (lower.order >= upper.order) lower else upper
		// A mouth kit's inside, teeth and tongue fitted to this mouth when there is one; else a plain dark opening.
		val generated = MouthKit.load()?.fit(upper, back.order, openLeft, openWidth, mid)
			?: listOfNotNull(insideLayer(upper, lower, openLeft, openWidth, mid, darken(lineColor(upper, lower))))
		if (generated.isEmpty()) return art
		val layers = art.layers.toMutableList().apply { addAll(indexOfFirst { it === back } + 1, generated) }
		return object : SourceArt by art { override val layers: List<SourceLayer> = layers }
	}

	/**
	 * A mouth kit: the inside, teeth and tongue of a mouth drawn shut over them (`mouth inside`, `upper teeth`,
	 * `lower teeth`, `tongue`, ..., with the `upper lip line` and `lower lip line` they were drawn behind), read from
	 * the PSD at `PSD2LIVE_MOUTH_KIT` or `~/.psd2live/mouth-kit.psd`. [fit] scales its parts to another shut mouth by the
	 * corners of the two mouths' shut lines and bends them along that mouth's line, so the kit's open proportions,
	 * teeth and tongue come with it.
	 */
	internal class MouthKit private constructor(
		private val left: Float, private val width: Float, private val mid: (Float) -> Float,
		private val parts: List<Pair<SourceLayer, String>>,
		/** Per place along the mouth (u in 0..1, [ROUND_SAMPLES] steps), how far the kit's inside reaches above and below its line. */
		private val above: FloatArray, private val below: FloatArray,
	) {
		/**
		 * At u, how much the kit is stretched up (or down) so its inside becomes an oval of the same height: the open mouth
		 * rounds toward the corners whatever the kit's own shape (a drawn open mouth is often a rounded box).
		 */
		private fun round(u: Float, up: Boolean): Float {
			val reach = if (up) above else below
			val k = (u.coerceIn(0f, 1f) * (reach.size - 1)).toInt()
			val oval = reach.max() * kotlin.math.sqrt(max(0f, 1f - (2f * u - 1f).let { it * it }))
			return (oval / reach[k].coerceAtLeast(1f)).coerceIn(0.2f, 3f)
		}

		/** The kit's parts fitted to the shut mouth from [left] across [width] whose line's middle is [targetMid]. */
		fun fit(owner: SourceLayer, order: Int, targetLeft: Float, targetWidth: Float, targetMid: (Float) -> Float): List<SourceLayer> {
			val s = targetWidth / width
			return parts.mapIndexedNotNull { k, (part, name) ->
				warp(part, s, targetLeft, targetMid)?.let { (bounds, raster) ->
					Generated(LayerId("${owner.id.raw}::mouth-kit-$k"), name, owner.groupPath, order, bounds, raster)
				}
			}
		}

		/** [part] moved onto the target: x scaled from the corners, y kept at its height above or below the line, scaled. */
		private fun warp(part: SourceLayer, s: Float, tLeft: Float, tMid: (Float) -> Float): Pair<LayerBounds, LayerRaster>? {
			val pb = part.bounds; val raster = part.raster
			fun toTargetX(xk: Float) = tLeft + (xk - left) * s
			fun toKitX(xt: Float) = left + (xt - tLeft) / s
			val tx0 = floor(toTargetX(pb.left.toFloat())).toInt() - 1; val tx1 = ceil(toTargetX((pb.left + pb.width).toFloat())).toInt() + 1
			val kitMids = (0..pb.width).map { mid(pb.left + it.toFloat()) }
			val tMids = (tx0..tx1).map { tMid(it + 0.5f) }
			val ty0 = floor(tMids.min() + (pb.top - kitMids.max()) * s * 3f).toInt() - 1
			val ty1 = ceil(tMids.max() + (pb.top + pb.height - kitMids.min()) * s * 3f).toInt() + 1
			val w = tx1 - tx0; val h = ty1 - ty0
			if (w <= 0 || h <= 0) return null
			val out = ByteArray(w * h * 4)
			val n = 3  // samples per side: the kit is drawn larger, so each target pixel averages several kit points
			var opaque = false
			for (y in 0 until h) for (x in 0 until w) {
				var r = 0f; var g = 0f; var b = 0f; var a = 0f
				for (j in 0 until n) for (i in 0 until n) {
					val xt = tx0 + x + (i + 0.5f) / n; val yt = ty0 + y + (j + 0.5f) / n
					val xk = toKitX(xt); val d = yt - tMid(xt)
					val yk = mid(xk) + d / (s * round((xk - left) / width, d < 0f))
					val px = floor(xk - pb.left).toInt(); val py = floor(yk - pb.top).toInt()
					if (px !in 0 until raster.width || py !in 0 until raster.height) continue
					val p = (py * raster.width + px) * 4
					val pa = (raster.rgba[p + 3].toInt() and 0xff) / 255f
					r += (raster.rgba[p].toInt() and 0xff) * pa; g += (raster.rgba[p + 1].toInt() and 0xff) * pa
					b += (raster.rgba[p + 2].toInt() and 0xff) * pa; a += pa
				}
				if (a <= 0f) continue
				val i = (y * w + x) * 4
				out[i] = (r / a).toInt().toByte(); out[i + 1] = (g / a).toInt().toByte(); out[i + 2] = (b / a).toInt().toByte()
				out[i + 3] = (255 * a / (n * n)).toInt().coerceIn(0, 255).toByte()
				opaque = true
			}
			return if (opaque) LayerBounds(tx0, ty0, w, h) to LayerRaster(w, h, out) else null
		}

		companion object {
			private const val ROUND_SAMPLES = 65
			@Volatile private var cached: Pair<String, MouthKit?>? = null

			/** The kit at `PSD2LIVE_MOUTH_KIT` or `~/.psd2live/mouth-kit.psd`, read once per file version; null when there is none. */
			fun load(): MouthKit? {
				val explicit = System.getenv("PSD2LIVE_MOUTH_KIT")
				if (explicit == null && !enabled) return null
				val path = (explicit ?: "${System.getProperty("user.home")}/.psd2live/mouth-kit.psd")
					.let { java.nio.file.Path.of(it) }
				if (!java.nio.file.Files.isRegularFile(path)) return null
				val key = "$path@${java.nio.file.Files.getLastModifiedTime(path).toMillis()}"
				cached?.let { (k, kit) -> if (k == key) return kit }
				val kit = runCatching { read(org.umamo.format.psd.PsdReader.read(java.nio.file.Files.readAllBytes(path))) }.getOrNull()
				cached = key to kit
				return kit
			}

			private fun read(art: SourceArt): MouthKit? {
				val named = art.layers.map { layer ->
					val semantic = LayerClassifier.classify(layer.name)
					Triple(layer, semantic.tag, PartRoles.fromName(layer.name, layer.groupPath, semantic.tag))
				}
				val upper = named.firstOrNull { it.third == PartRole.UPPER_LINE }?.first ?: return null
				val lower = named.firstOrNull { it.third == PartRole.LOWER_LINE }?.first ?: return null
				val up = stroke(upper) ?: return null
				val lo = stroke(lower) ?: return null
				val inside = named.filter { (layer, tag, role) -> tag in MOUTHS && role == null && opaque(layer) }.maxByOrNull { area(it.first) }?.first
					?: return null
				// The kit's corners are its inside's ends, not its lines' (a line may stop short of the mouth's corner, its
				// skin covering the rest): fitted to another mouth's line ends, the inside then reaches exactly to them.
				val ir = inside.raster; val ib = inside.bounds
				val columns = (0 until ir.width).filter { x -> (0 until ir.height).any { y -> alpha(ir, y * ir.width + x) >= 128 } }
				if (columns.isEmpty()) return null
				val left = ib.left + (columns.first() + 0.5f) * ib.width / ir.width
				val width = (columns.last() - columns.first()) * ib.width / ir.width.toFloat()
				if (width < 8f) return null
				// Behind to front as a mouth is drawn: the inside, then the teeth, then the tongue and its line.
				val rank = mapOf(SemanticTag.TOOTH_T to 1, SemanticTag.TOOTH_B to 1, SemanticTag.TONGUE to 2)
				val parts = listOf(inside to "mouth inside (kit)") + named.filter { it.second in rank && opaque(it.first) }
					.sortedBy { rank.getValue(it.second) }
					.map { (layer, tag, _) ->
						layer to when (tag) {
							SemanticTag.TOOTH_T -> "upper teeth (kit)"
							SemanticTag.TOOTH_B -> "lower teeth (kit)"
							else -> if (layer.name.contains("line", ignoreCase = true)) "tongue line (kit)" else "tongue (kit)"
						}
					}
				val kitMid = chord(left, width) { x -> 0.5f * (at(up, x) + at(lo, x)) }
				val above = FloatArray(ROUND_SAMPLES); val below = FloatArray(ROUND_SAMPLES)
				for (k in 0 until ROUND_SAMPLES) {
					val x = left + width * k / (ROUND_SAMPLES - 1f)
					val col = ((x - ib.left) * ir.width / ib.width).toInt().coerceIn(0, ir.width - 1)
					val ys = (0 until ir.height).filter { alpha(ir, it * ir.width + col) >= 128 }
					if (ys.isEmpty()) continue
					val sy = ib.height / ir.height.toFloat()
					above[k] = max(0f, kitMid(x) - (ib.top + ys.first() * sy)); below[k] = max(0f, ib.top + (ys.last() + 1) * sy - kitMid(x))
				}
				return MouthKit(left, width, kitMid, parts, above, below)
			}
		}
	}

	private data class P(val x: Float, val y: Float)

	/** The straight line through [line]'s ends, from [left] across [width]. */
	private fun chord(left: Float, width: Float, line: (Float) -> Float): (Float) -> Float {
		val a = line(left); val b = line(left + width)
		return { x -> a + (b - a) * (x - left) / width.coerceAtLeast(1f) }
	}

	private fun opaque(layer: SourceLayer): Boolean = layer.raster.rgba.let { rgba -> (3 until rgba.size step 4).any { (rgba[it].toInt() and 0xff) >= 128 } }
	private fun area(layer: SourceLayer) = layer.bounds.width.toLong() * layer.bounds.height

	/** [layer]'s dark stroke: per canvas column, the middle of its pixels well darker than the layer's light colour. */
	private fun stroke(layer: SourceLayer): List<P>? {
		val raster = layer.raster; val b = layer.bounds
		val w = raster.width; val h = raster.height
		if (w <= 0 || h <= 0) return null
		val sx = b.width / w.toFloat(); val sy = b.height / h.toFloat()
		val lums = ArrayList<Float>()
		for (p in 0 until w * h) if (alpha(raster, p) >= 128) lums += lum(raster, p)
		if (lums.size < 8) return null
		lums.sort()
		val light = lums[(lums.size * 0.85f).toInt().coerceAtMost(lums.size - 1)]; val dark = lums[(lums.size * 0.02f).toInt()]
		if (light - dark < 0.15f) return null
		val cutoff = dark + 0.5f * (light - dark)
		val out = ArrayList<P>()
		for (x in 0 until w) {
			var sum = 0f; var n = 0
			for (y in 0 until h) { val p = y * w + x; if (alpha(raster, p) >= 128 && lum(raster, p) <= cutoff) { sum += y; n++ } }
			if (n > 0) out += P(b.left + (x + 0.5f) * sx, b.top + (sum / n + 0.5f) * sy)
		}
		return out.takeIf { it.size >= 4 }
	}

	/** The stroke's height at canvas [x], held level past its ends. */
	private fun at(s: List<P>, x: Float): Float {
		if (x <= s.first().x) return s.first().y
		if (x >= s.last().x) return s.last().y
		val k = s.indexOfFirst { it.x >= x }
		val a = s[k - 1]; val c = s[k]
		return a.y + (c.y - a.y) * ((x - a.x) / (c.x - a.x).coerceAtLeast(1e-4f))
	}

	private fun alpha(r: LayerRaster, p: Int) = r.rgba[p * 4 + 3].toInt() and 0xff
	private fun lum(r: LayerRaster, p: Int) = (0.2126f * (r.rgba[p * 4].toInt() and 0xff) + 0.7152f * (r.rgba[p * 4 + 1].toInt() and 0xff) +
		0.0722f * (r.rgba[p * 4 + 2].toInt() and 0xff)) / 255f

	/**
	 * The lines' own colour: the mean of the pixels well darker than each layer's light colour - the line, not the band of
	 * skin it is drawn on, however little of the layer the line covers.
	 */
	private fun lineColor(vararg layers: SourceLayer): Triple<Float, Float, Float> {
		var r = 0.0; var g = 0.0; var bl = 0.0; var n = 0
		for (layer in layers) {
			val raster = layer.raster
			val opaque = (0 until raster.width * raster.height).filter { alpha(raster, it) >= 128 }
			val lums = opaque.map { lum(raster, it) }.sorted()
			if (lums.size < 8) continue
			val light = lums[(lums.size * 0.85f).toInt().coerceAtMost(lums.size - 1)]; val dark = lums[(lums.size * 0.02f).toInt()]
			val cutoff = dark + 0.35f * (light - dark)
			for (p in opaque) if (lum(raster, p) <= cutoff) {
				r += raster.rgba[p * 4].toInt() and 0xff; g += raster.rgba[p * 4 + 1].toInt() and 0xff; bl += raster.rgba[p * 4 + 2].toInt() and 0xff; n++
			}
		}
		return if (n == 0) Triple(150f, 40f, 55f) else Triple((r / n).toFloat(), (g / n).toFloat(), (bl / n).toFloat())
	}

	/** A mouth's dark: the line's hue, deeper. */
	private fun darken(c: Triple<Float, Float, Float>): Triple<Float, Float, Float> {
		// Half way to a usual mouth's red, so a dark or brown line still opens onto a red mouth, not a black one.
		val red = Triple(175f, 55f, 70f)
		fun mix(a: Float, b: Float) = 0.45f * a + 0.55f * b
		return Triple(mix(c.first, red.first) * 0.9f, mix(c.second, red.second) * 0.75f + 10f, mix(c.third, red.third) * 0.75f + 15f)
	}

	private fun insideLayer(upper: SourceLayer, lower: SourceLayer, left: Float, width: Float, mid: (Float) -> Float,
	                        color: Triple<Float, Float, Float>): SourceLayer? {
		val n = 64
		fun s(t: Float) = sin(PI.toFloat() * t).coerceAtLeast(0f).pow(0.75f)
		val top = (0..n).map { k -> val t = k / n.toFloat(); val x = left + width * t; P(x, mid(x) - UPPER_REACH * width * s(t)) }
		val bottom = (0..n).map { k -> val t = k / n.toFloat(); val x = left + width * t; P(x, mid(x) + LOWER_REACH * width * s(t)) }
		val all = top + bottom
		val x0 = floor(all.minOf { it.x }).toInt() - 2; val y0 = floor(all.minOf { it.y }).toInt() - 2
		val x1 = ceil(all.maxOf { it.x }).toInt() + 2; val y1 = ceil(all.maxOf { it.y }).toInt() + 2
		val w = x1 - x0; val h = y1 - y0
		if (w <= 0 || h <= 0) return null
		val image = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
		val g = image.createGraphics()
		try {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
			g.color = java.awt.Color(color.first.toInt().coerceIn(0, 255), color.second.toInt().coerceIn(0, 255), color.third.toInt().coerceIn(0, 255))
			val path = Path2D.Float()
			path.moveTo(top[0].x - x0, top[0].y - y0)
			for (p in top.drop(1)) path.lineTo(p.x - x0, p.y - y0)
			for (p in bottom.reversed()) path.lineTo(p.x - x0, p.y - y0)
			path.closePath()
			g.fill(path)
		} finally {
			g.dispose()
		}
		val rgba = ByteArray(w * h * 4)
		for (y in 0 until h) for (x in 0 until w) {
			val argb = image.getRGB(x, y); val i = (y * w + x) * 4
			rgba[i] = (argb shr 16).toByte(); rgba[i + 1] = (argb shr 8).toByte(); rgba[i + 2] = argb.toByte(); rgba[i + 3] = (argb ushr 24).toByte()
		}
		val behindOf = if (lower.order >= upper.order) lower else upper
		return Generated(LayerId("${upper.id.raw}::mouth-inside"), "mouth inside (generated)", upper.groupPath,
			behindOf.order, LayerBounds(x0, y0, w, h), LayerRaster(w, h, rgba))
	}

	/** A layer this synthesis drew: meshed as a strip along the mouth ([stripMesh]) rather than traced round. */
	interface Synthesized : SourceLayer

	/**
	 * [layer]'s mesh as a strip of columns across the mouth, every 1/40 of its width, each spanning the column's paint top
	 * to bottom in three bands. A traced mesh of a thin, wide mouth part is a dozen vertices and bends in straight pieces;
	 * this one bends as smoothly as the lips do. Null when the layer paints nothing.
	 */
	internal fun stripMesh(layer: SourceLayer): io.github.psd2live.core.AdaptiveMeshGenerator.Result? {
		val raster = layer.raster; val w = raster.width; val h = raster.height
		if (w <= 1 || h <= 1) return null
		val columns = 40.coerceAtMost(w)
		val rows = 4
		val positions = ArrayList<Float>(); val indices = ArrayList<Int>()
		val loops = ArrayList<IntArray>()
		var previous: IntArray? = null
		var top = ArrayList<Int>(); var bottom = ArrayList<Int>()
		fun closeLoop() {
			if (top.size >= 2) loops += (top + bottom.reversed()).toIntArray()
			top = ArrayList(); bottom = ArrayList(); previous = null
		}
		for (c in 0..columns) {
			val xv = w * c / columns.toFloat()
			val x0 = (xv - 0.5f * w / columns).toInt().coerceIn(0, w - 1); val x1 = (xv + 0.5f * w / columns).toInt().coerceIn(x0, w - 1)
			var minY = Int.MAX_VALUE; var maxY = -1
			for (x in x0..x1) for (y in 0 until h) if ((raster.rgba[(y * w + x) * 4 + 3].toInt() and 0xff) > 8) { if (y < minY) minY = y; if (y > maxY) maxY = y }
			if (maxY < 0) { closeLoop(); continue }
			val y0 = minY - 0.5f; val y1 = maxY + 1.5f
			val column = IntArray(rows) { r ->
				positions += xv; positions += y0 + (y1 - y0) * r / (rows - 1)
				positions.size / 2 - 1
			}
			previous?.let { p -> for (r in 0 until rows - 1) {
				indices += listOf(p[r], column[r], p[r + 1]); indices += listOf(column[r], column[r + 1], p[r + 1])
			} }
			top += column.first(); bottom += column.last()
			previous = column
		}
		closeLoop()
		if (indices.isEmpty()) return null
		return io.github.psd2live.core.AdaptiveMeshGenerator.Result(positions.toFloatArray(), indices.toIntArray(), loops)
	}

	private class Generated(
		override val id: LayerId, override val name: String, override val groupPath: String, override val order: Int,
		override val bounds: LayerBounds, override val raster: LayerRaster,
	) : Synthesized {
		override val opacity: Float get() = 1f
		override val clipped: Boolean get() = false
		override val blend: LayerBlend get() = LayerBlend.Normal
	}
}
