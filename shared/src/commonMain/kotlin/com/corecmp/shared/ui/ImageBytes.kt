package com.corecmp.shared.ui

import coil3.request.ImageRequest
import coil3.size.Precision
import coil3.size.Size
import coil3.svg.SvgDecoder
import com.corecmp.shared.api.CoreCmpLogger
import com.corecmp.shared.api.HttpClientProvider
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import io.ktor.http.isSuccess
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.cancellation.CancellationException
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private const val MAX_IMAGE_CACHE_ENTRIES = 48
private val remoteImageBytesCache = LinkedHashMap<String, ByteArray>(MAX_IMAGE_CACHE_ENTRIES)
private val remoteImageMutex = Mutex()

internal fun redactImageUrl(url: String): String = url.substringBefore('?')

private fun MutableMap<String, ByteArray>.putBounded(key: String, value: ByteArray) {
    this[key] = value
    val overflow = size - MAX_IMAGE_CACHE_ENTRIES
    if (overflow <= 0) return
    keys.filter { it != key }.take(overflow).forEach { remove(it) }
}

internal fun isPngOrJpeg(bytes: ByteArray): Boolean {
    if (bytes.size >= 3 &&
        bytes[0] == 0x89.toByte() &&
        bytes[1] == 0x50.toByte() &&
        bytes[2] == 0x4E.toByte()
    ) {
        return true
    }
    return bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()
}

internal fun isWebp(bytes: ByteArray): Boolean =
    bytes.size >= 12 &&
        bytes[0] == 0x52.toByte() &&
        bytes[1] == 0x49.toByte() &&
        bytes[2] == 0x46.toByte() &&
        bytes[8] == 0x57.toByte() &&
        bytes[9] == 0x45.toByte() &&
        bytes[10] == 0x42.toByte() &&
        bytes[11] == 0x50.toByte()

internal fun isRasterImageBytes(bytes: ByteArray): Boolean =
    isPngOrJpeg(bytes) || isWebp(bytes)

internal fun looksLikeSvgBytes(bytes: ByteArray): Boolean {
    val head = runCatching {
        bytes.decodeToString(0, minOf(bytes.size, 256))
    }.getOrNull().orEmpty()
    val trimmed = head.trimStart()
    return trimmed.startsWith("<svg", ignoreCase = true) ||
        (trimmed.startsWith("<?xml", ignoreCase = true) && head.contains("<svg", ignoreCase = true))
}

private val SVG_OPEN_TAG = Regex("""<svg\b[^>]*>""", RegexOption.IGNORE_CASE)
private val SVG_VIEWBOX = Regex("""viewBox\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
private val SVG_WIDTH_ATTR = Regex("""\bwidth\s*=\s*["']([\d.]+)""", RegexOption.IGNORE_CASE)
private val SVG_HEIGHT_ATTR = Regex("""\bheight\s*=\s*["']([\d.]+)""", RegexOption.IGNORE_CASE)
private val SVG_FONT_FAMILY = Regex("""font-family="[^"]*"""", RegexOption.IGNORE_CASE)

internal fun svgIntrinsicSize(bytes: ByteArray): Pair<Int, Int>? {
    val head = runCatching {
        bytes.decodeToString(0, minOf(bytes.size, 1024))
    }.getOrNull() ?: return null
    val tag = SVG_OPEN_TAG.find(head)?.value ?: return null
    val vbParts = SVG_VIEWBOX.find(tag)?.groupValues?.get(1)?.trim()?.split(Regex("""[\s,]+"""))
    val rawW: Float
    val rawH: Float
    if (vbParts != null && vbParts.size == 4) {
        rawW = vbParts[2].toFloatOrNull() ?: return null
        rawH = vbParts[3].toFloatOrNull() ?: return null
    } else {
        rawW = SVG_WIDTH_ATTR.find(tag)?.groupValues?.get(1)?.toFloatOrNull() ?: return null
        rawH = SVG_HEIGHT_ATTR.find(tag)?.groupValues?.get(1)?.toFloatOrNull() ?: return null
    }
    if (rawW <= 0f || rawH <= 0f) return null
    val longest = maxOf(rawW, rawH)
    val scale = if (longest > 512f) 512f / longest else 1f
    return (rawW * scale).toInt().coerceAtLeast(1) to (rawH * scale).toInt().coerceAtLeast(1)
}

internal fun simplifySvgFonts(bytes: ByteArray): ByteArray {
    val text = runCatching { bytes.decodeToString() }.getOrNull() ?: return bytes
    if (!text.contains("font-family", ignoreCase = true)) return bytes
    return SVG_FONT_FAMILY.replace(text, """font-family="sans-serif"""").encodeToByteArray()
}

internal fun prepareRemoteImageBytes(bytes: ByteArray, url: String): ByteArray {
    val unwrapped = unwrapEmbeddedRaster(bytes, url)
    if (isRasterImageBytes(unwrapped)) return unwrapped
    if (!looksLikeSvgBytes(unwrapped)) return unwrapped
    return simplifySvgFonts(inlineSvgCssClasses(unwrapped))
}

private val SVG_STYLE_BLOCK =
    Regex("""<style[^>]*>([\s\S]*?)</style>""", RegexOption.IGNORE_CASE)
private val SVG_CSS_RULE =
    Regex("""\.([A-Za-z_][A-Za-z0-9_-]*)\s*\{([^}]*)\}""")
private val SVG_CLASS_ATTR =
    Regex("""\sclass="([^"]+)"""", RegexOption.IGNORE_CASE)
private val SVG_INLINE_CSS_PROPS = setOf(
    "fill", "stroke", "stroke-width", "opacity", "fill-opacity", "stroke-opacity",
)

/** iOS Skia ignores CSS classes; Illustrator SVGs often use `.st0 { fill: ... }`. */
internal fun inlineSvgCssClasses(bytes: ByteArray): ByteArray {
    val text = runCatching { bytes.decodeToString() }.getOrNull() ?: return bytes
    if (!text.contains("<style", ignoreCase = true)) return bytes
    val classAttrs = linkedMapOf<String, List<Pair<String, String>>>()
    val blocks = SVG_STYLE_BLOCK.findAll(text).toList()
    if (blocks.isEmpty()) return bytes
    val rewritten = StringBuilder()
    var cursor = 0
    for (block in blocks) {
        rewritten.append(text, cursor, block.range.first)
        rewritten.append(rewriteSvgStyleBlock(block.value, block.groupValues[1], classAttrs))
        cursor = block.range.last + 1
    }
    rewritten.append(text, cursor, text.length)
    if (classAttrs.isEmpty()) return bytes
    val out = SVG_CLASS_ATTR.replace(rewritten.toString()) { match ->
        val extra = match.groupValues[1]
            .split(Regex("""\s+"""))
            .filter { it.isNotEmpty() }
            .flatMap { classAttrs[it].orEmpty() }
            .joinToString("") { (name, value) -> """ $name="$value"""" }
        extra.ifEmpty { match.value }
    }
    return out.encodeToByteArray()
}

private fun rewriteSvgStyleBlock(
    fullTag: String,
    css: String,
    classAttrs: MutableMap<String, List<Pair<String, String>>>,
): String {
    val leftover = StringBuilder()
    var last = 0
    for (rule in SVG_CSS_RULE.findAll(css)) {
        leftover.append(css, last, rule.range.first)
        val className = rule.groupValues[1]
        val decls = rule.groupValues[2].split(';').map { it.trim() }.filter { it.isNotEmpty() }
        val inline = mutableListOf<Pair<String, String>>()
        val rest = mutableListOf<String>()
        for (decl in decls) {
            val split = decl.indexOf(':')
            if (split <= 0) {
                rest.add(decl)
                continue
            }
            val name = decl.substring(0, split).trim().lowercase()
            val value = decl.substring(split + 1).trim()
            if (name in SVG_INLINE_CSS_PROPS && value.isNotEmpty()) {
                inline.add(name to value)
            } else {
                rest.add(decl)
            }
        }
        if (inline.isNotEmpty()) {
            classAttrs[className] = classAttrs[className].orEmpty() + inline
        }
        if (rest.isNotEmpty()) {
            leftover.append('.').append(className).append('{')
                .append(rest.joinToString(";")).append('}')
        }
        last = rule.range.last + 1
    }
    leftover.append(css, last, css.length)
    val keptCss = leftover.toString()
    if (keptCss.isBlank()) return ""
    val innerStart = fullTag.indexOf('>') + 1
    val innerEnd = fullTag.lastIndexOf("</style>", ignoreCase = true)
    if (innerStart <= 0 || innerEnd < innerStart) return fullTag
    return fullTag.substring(0, innerStart) + keptCss + fullTag.substring(innerEnd)
}

internal fun imageBytesKind(bytes: ByteArray): String = when {
    bytes.size >= 3 &&
        bytes[0] == 0x89.toByte() &&
        bytes[1] == 0x50.toByte() &&
        bytes[2] == 0x4E.toByte() -> "png"
    bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte() -> "jpeg"
    isWebp(bytes) -> "webp"
    looksLikeSvgBytes(bytes) -> "svg"
    else -> "unknown"
}

/** Some "SVG" files are a PNG/JPEG wrapped in `<image href="data:…">`. */
@OptIn(ExperimentalEncodingApi::class)
internal fun unwrapEmbeddedRaster(bytes: ByteArray, url: String): ByteArray {
    if (isRasterImageBytes(bytes)) return bytes
    val path = url.substringBefore('?').lowercase()
    val head = runCatching {
        bytes.decodeToString(0, minOf(bytes.size, 512))
    }.getOrNull().orEmpty()
    val looksSvg = path.endsWith(".svg") ||
        head.contains("<svg", ignoreCase = true) ||
        head.trimStart().startsWith("<?xml", ignoreCase = true)
    if (!looksSvg || !head.contains("data:image", ignoreCase = true)) return bytes
    val asText = runCatching { bytes.decodeToString() }.getOrNull() ?: return bytes
    val match = EMBEDDED_RASTER_REGEX.find(asText) ?: return bytes
    val payload = match.groupValues.getOrNull(2)?.replace("\\s".toRegex(), "").orEmpty()
    if (payload.isBlank()) return bytes
    return runCatching { Base64.decode(payload) }.getOrNull()?.takeIf { it.isNotEmpty() } ?: bytes
}

private val EMBEDDED_RASTER_REGEX =
    Regex(
        """data:image/(png|jpeg|jpg|webp);base64,([A-Za-z0-9+/=\s]+)""",
        RegexOption.IGNORE_CASE,
    )

internal suspend fun fetchPreparedRemoteImageBytes(url: String): ByteArray? {
    remoteImageMutex.withLock {
        remoteImageBytesCache[url]?.let { return it }
    }
    return try {
        val response = HttpClientProvider.client.get(url)
        if (!response.status.isSuccess()) {
            CoreCmpLogger.d("CustomImage fetch failed url=${redactImageUrl(url)} status=${response.status}")
            return null
        }
        val bytes = response.readRawBytes()
        if (bytes.isEmpty()) {
            CoreCmpLogger.d("CustomImage fetch empty url=${redactImageUrl(url)}")
            return null
        }
        val decoded = prepareRemoteImageBytes(bytes, url)
        CoreCmpLogger.d(
            "CustomImage fetch url=${redactImageUrl(url)} status=${response.status} " +
                "raw=${imageBytesKind(bytes)}/${bytes.size} " +
                "decoded=${imageBytesKind(decoded)}/${decoded.size}",
        )
        remoteImageMutex.withLock { remoteImageBytesCache.putBounded(url, decoded) }
        decoded
    } catch (e: CancellationException) {
        throw e
    } catch (t: Throwable) {
        CoreCmpLogger.d("CustomImage fetch error url=${redactImageUrl(url)} ${t.message}")
        null
    }
}

internal fun ImageRequest.Builder.applyPreparedImageBytes(
    bytes: ByteArray,
    cacheKey: String,
): ImageRequest.Builder {
    data(bytes)
    memoryCacheKey(cacheKey)
    diskCacheKey(cacheKey)
    if (looksLikeSvgBytes(bytes) && !isRasterImageBytes(bytes)) {
        decoderFactory(SvgDecoder.Factory())
        svgIntrinsicSize(bytes)?.let { (w, h) ->
            size(Size(w, h))
            precision(Precision.EXACT)
        }
    }
    return this
}
