// SPDX-License-Identifier: GPL-3.0-or-later
package app.ciying.keyboard

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import java.io.Closeable

/** Only accessed on the dictionary worker, never the UI thread. */
class CandidateDictionary(context: Context) : Lexicon, Closeable {
    private val preferences = context.getSharedPreferences("word-frequency", Context.MODE_PRIVATE)
    private val database: SQLiteDatabase
    private val converter = PinyinConverter(this)
    init {
        val file = context.getDatabasePath("lexicon-v3.db")
        if (!file.exists()) {
            file.parentFile?.mkdirs()
            val temporary = java.io.File(file.parentFile, "lexicon-v3.copying")
            context.assets.open("lexicon-v3.db").use { input -> temporary.outputStream().use { input.copyTo(it) } }
            check(temporary.renameTo(file)) { "Cannot install offline lexicon" }
        }
        database = SQLiteDatabase.openDatabase(file.path, null, SQLiteDatabase.OPEN_READONLY)
    }
    fun lookup(input: String, before: String = "", correction: Boolean = true, personalized: Boolean = true) = converter.lookup(input, before, correction, personalized)
    override fun exact(keys: List<String>): List<LexiconEntry> {
        if (keys.isEmpty()) return emptyList()
        val placeholders = keys.joinToString(",") { "?" }
        return database.rawQuery("SELECT key,pinyin,word,frequency,gloss FROM lexicon WHERE key IN ($placeholders) ORDER BY frequency DESC LIMIT 512", keys.toTypedArray()).use { it.entries() }
    }
    override fun completions(key: String, initials: Boolean): List<LexiconEntry> {
        val where = if (initials) "initials = ?" else "key >= ? AND key < ?"
        val arguments = if (initials) arrayOf(key) else arrayOf(key, "$key{")
        return database.rawQuery("SELECT key,pinyin,word,frequency,gloss FROM lexicon WHERE $where ORDER BY frequency DESC LIMIT 16", arguments).use { it.entries() }
    }
    private fun Cursor.entries(): List<LexiconEntry> = buildList {
        while (moveToNext()) add(LexiconEntry(getString(0), getString(1), getString(2), getLong(3), getString(4)))
    }
    override fun learned(key: String, word: String) = preferences.getInt("$key|$word", 0)
    override fun transition(before: String, next: String): Double {
        val tail = before.takeLastWhile { it in '\u3400'..'\u9fff' }.takeLast(8)
        if (tail.isEmpty()) return 0.0
        val suffixes = (1..tail.length).map { tail.takeLast(it) }
        val marks = suffixes.joinToString(",") { "?" }
        val weight = database.rawQuery("SELECT MAX(weight) FROM context_pairs WHERE previous IN ($marks) AND next=?", (suffixes + next).toTypedArray()).use {
            if (it.moveToFirst() && !it.isNull(0)) it.getLong(0) else 0L
        }
        val personal = suffixes.maxOfOrNull { preferences.getInt("context:$it|$next", 0) } ?: 0
        return (kotlin.math.ln(1.0 + weight) * 1.2 + kotlin.math.ln(1.0 + personal) * 2.0).coerceAtMost(10.0)
    }
    fun remember(key: String, item: Candidate, before: String = "") {
        val identity = "${item.reading ?: key.take(item.consumed)}|${item.hanzi}"
        if (preferences.contains(identity) || preferences.all.size < 2000) {
            preferences.edit().putInt(identity, (preferences.getInt(identity, 0) + 1).coerceAtMost(100)).apply()
        }
        var previous = before.takeLastWhile { it in '\u3400'..'\u9fff' }.takeLast(8)
        for (word in item.segments) {
            if (previous.isNotEmpty()) {
                val pair = "context:$previous|$word"
                if (preferences.contains(pair) || preferences.all.size < 4000) preferences.edit()
                    .putInt(pair, (preferences.getInt(pair, 0) + 1).coerceAtMost(100)).apply()
            }
            previous = word
        }
    }
    override fun close() = database.close()
}
