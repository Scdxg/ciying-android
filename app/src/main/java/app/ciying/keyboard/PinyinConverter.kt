// SPDX-License-Identifier: GPL-3.0-or-later
package app.ciying.keyboard

import java.util.Locale
import kotlin.math.ln

data class Candidate(val hanzi: String, val gloss: String, val consumed: Int,
                     val correctedFrom: String? = null, val reading: String? = null,
                     val segments: List<String> = listOf(hanzi))
data class LexiconEntry(val key: String, val pinyin: String, val word: String, val frequency: Long, val gloss: String)
interface Lexicon {
    fun exact(keys: List<String>): List<LexiconEntry>
    fun completions(key: String, initials: Boolean): List<LexiconEntry>
    fun learned(key: String, word: String): Int = 0
    fun transition(before: String, next: String): Double = 0.0
}

/** Word-lattice conversion, separated from Android and translation rendering.
 * References: Qingjian parser/dictionary/sentence modules. This implementation uses
 * unigram beam search; the upstream bigram and neural models are not ported.
 */
class PinyinConverter(private val lexicon: Lexicon) {
    fun lookup(raw: String, before: String = "", correction: Boolean = true, personalized: Boolean = true): List<Candidate> {
        val input = raw.lowercase(Locale.ROOT).replace("u:", "v").replace("ü", "v")
        if (input.isEmpty() || input.length > 64 || input.any { it !in 'a'..'z' }) return emptyList()
        val cache = HashMap<Int, List<LexiconEntry>>()
        val transitions = HashMap<Pair<String, String>, Double>()
        fun contextScore(previous: String, next: String) = if (!personalized) 0.0 else transitions.getOrPut(previous to next) { lexicon.transition(previous, next) }
        fun wordScore(entry: LexiconEntry) = ln(entry.frequency.coerceAtLeast(1).toDouble()) - ln(100_000_000.0) +
            ln(1.0 + if (personalized) lexicon.learned(entry.key, entry.word) else 0) * 2.0
        fun spans(start: Int): List<LexiconEntry> = cache.getOrPut(start) {
            val keys = (start + 1..minOf(input.length, start + 24)).map { input.substring(start, it) }
            lexicon.exact(keys).groupBy { it.key }.values.flatMap { it.take(6) }
        }
        fun candidate(entry: LexiconEntry, consumed: Int) = Candidate(entry.word, entry.gloss, consumed, reading = entry.key)
        val exact = lexicon.exact(listOf(input)).sortedByDescending { wordScore(it) + contextScore(before, it.word) }
        val result = exact.map { candidate(it, input.length) }.toMutableList()
        if (input.length >= 4) {
            data class Path(val words: List<LexiconEntry>, val score: Double)
            val paths = Array(input.length + 1) { mutableListOf<Path>() }
            paths[0].add(Path(emptyList(), 0.0))
            for (start in input.indices) {
                if (paths[start].isEmpty()) continue
                for (entry in spans(start)) {
                    val end = start + entry.key.length
                    for (path in paths[start].take(6)) {
                        val previous = path.words.lastOrNull()?.word ?: before
                        paths[end].add(Path(path.words + entry, path.score + wordScore(entry) + contextScore(previous, entry.word)))
                    }
                    val best = paths[end].sortedByDescending { it.score }.distinctBy { it.words.joinToString("") { word -> word.word } }.take(6)
                    paths[end].clear(); paths[end].addAll(best)
                }
            }
            val sentences = paths.last().sortedByDescending { it.score }.take(3).map { path ->
                Candidate(path.words.joinToString("") { it.word }, path.words.map { it.gloss }.filter { it.isNotBlank() }.joinToString(" · "), input.length,
                    reading = input, segments = path.words.map { it.word })
            }
            if (exact.isEmpty()) result.addAll(0, sentences)
        }
        // Never replace an exact valid reading. Edits are bounded to one keyboard error.
        if (exact.isEmpty() && correction && input.length in 4..24) {
            val variants = typoVariants(input)
            val corrected = variants.chunked(200).flatMap { lexicon.exact(it) }
                .sortedByDescending { wordScore(it) + contextScore(before, it.word) }
                .distinctBy { it.word }.take(8)
                .map { Candidate(it.word, it.gloss, input.length, input, it.key) }
            if (result.isEmpty()) result.addAll(corrected) else result.addAll(minOf(3, result.size), corrected)
        }
        if (input.length in 2..12) result += lexicon.completions(input, true).map { candidate(it, input.length) }
        if (result.size < 12) result += lexicon.completions(input, false).map { candidate(it, input.length) }
        result += spans(0).filter { it.key.length < input.length }
            .sortedWith(compareByDescending<LexiconEntry> { it.key.length }.thenByDescending { it.frequency })
            .map { candidate(it, it.key.length) }
        return result.distinctBy { it.hanzi to it.consumed }.take(36)
    }

    private fun typoVariants(input: String): List<String> {
        val result = LinkedHashSet<String>()
        val neighbors = mapOf('q' to "wa", 'w' to "qeas", 'e' to "wrsd", 'r' to "etdf", 't' to "ryfg", 'y' to "tugh", 'u' to "yihj", 'i' to "uojk", 'o' to "ipkl", 'p' to "ol", 'a' to "qwsz", 's' to "awedxz", 'd' to "serfcx", 'f' to "drtgvc", 'g' to "ftyhbv", 'h' to "gyujnb", 'j' to "huikmn", 'k' to "jiolm", 'l' to "kop", 'z' to "asx", 'x' to "zsdc", 'c' to "xdfv", 'v' to "cfgb", 'b' to "vghn", 'n' to "bhjm", 'm' to "njk")
        for (i in input.indices) {
            result.add(input.removeRange(i, i + 1))
            if (i + 1 < input.length) result.add(input.take(i) + input[i + 1] + input[i] + input.drop(i + 2))
            for (letter in neighbors[input[i]].orEmpty()) result.add(input.take(i) + letter + input.drop(i + 1))
        }
        for (i in 0..input.length) for (letter in 'a'..'z') result.add(input.take(i) + letter + input.drop(i))
        result.remove(input)
        return result.take(1000)
    }
}
