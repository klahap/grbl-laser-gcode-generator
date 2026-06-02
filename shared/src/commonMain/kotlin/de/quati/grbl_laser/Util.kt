package de.quati.grbl_laser

fun String.toPathSafe(): String {
    val umlautMap = mapOf(
        'ä' to "ae", 'ö' to "oe", 'ü' to "ue",
        'Ä' to "Ae", 'Ö' to "Oe", 'Ü' to "Ue",
        'ß' to "ss"
    )
    return this
        .map { char -> umlautMap[char]?.let { return@map it } ?: char.toString() }
        .joinToString("")
        .let { java.text.Normalizer.normalize(it, java.text.Normalizer.Form.NFD) }
        .replace(Regex("\\p{InCombiningDiacriticalMarks}"), "") // strip remaining accents
        .replace(Regex("[^a-zA-Z0-9._-]"), "_")                 // replace specials with _
        .replace(Regex("_+"), "_")                              // collapse multiple underscores
        .trim('_')                                                           // strip leading/trailing _
}