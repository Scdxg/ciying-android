package app.ciying.keyboard

import org.junit.Assert.*
import org.junit.Test

class PinyinConverterTest {
    private val entries = listOf(
        LexiconEntry("tuoxie", "tuo xie", "拖鞋", 3692, "n. slippers"),
        LexiconEntry("tuoxie", "tuo xie", "妥协", 1700, "v. compromise"),
        LexiconEntry("wo", "wo", "我", 500000, "I"),
        LexiconEntry("xuexi", "xue xi", "学习", 31925, "study"),
        LexiconEntry("yingyu", "ying yu", "英语", 20000, "English"),
        LexiconEntry("nihao", "ni hao", "你好", 70000, "hello"),
        LexiconEntry("shijian", "shi jian", "时间", 100000, "time"),
        LexiconEntry("shijian", "shi jian", "实践", 1700, "practice"),
        LexiconEntry("canjia", "can jia", "参加", 40000, "participate"),
        LexiconEntry("lvse", "lv se", "绿色", 5000, "green")
    )
    private fun engine(bonus: Int = 0) = PinyinConverter(object : Lexicon {
        override fun exact(keys: List<String>) = entries.filter { it.key in keys }.sortedByDescending { it.frequency }
        override fun completions(key: String, initials: Boolean) = entries.filter {
            if (initials) it.pinyin.split(' ').joinToString("") { s -> s.take(1) } == key else it.key.startsWith(key)
        }.sortedByDescending { it.frequency }
        override fun learned(key: String, word: String) = if (word == "妥协") bonus else 0
        override fun transition(before: String, next: String) = if (before.endsWith("参加") && next == "实践") 9.0 else 0.0
    })
    @Test fun specialNameHasFixedRankAndCompleteDescription() {
        val converter = PinyinConverter(object : Lexicon {
            override fun exact(keys: List<String>) = emptyList<LexiconEntry>()
            override fun completions(key: String, initials: Boolean) =
                if (initials && key == "gzh") listOf(LexiconEntry("gongzhonghao", "gong zhong hao", "公众号", 1000, "public account"))
                else emptyList()
            override fun learned(key: String, word: String) = 100
        })
        val full = converter.lookup("gaozihao").first()
        assertEquals("高梓皓", full.hanzi)
        assertEquals(SPECIAL_GAOZIHAO_TEXT, full.gloss)
        assertEquals(8, full.consumed)
        val abbreviated = converter.lookup("gzh")
        assertEquals("公众号", abbreviated[0].hanzi)
        assertEquals("高梓皓", abbreviated[1].hanzi)
        assertEquals(3, abbreviated[1].consumed)
        assertEquals(SPECIAL_GAOZIHAO_TEXT, abbreviated[1].gloss)
        assertEquals(1, abbreviated.count { it.hanzi == "高梓皓" })
        assertTrue(converter.lookup("gaozi").none { it.gloss == SPECIAL_GAOZIHAO_TEXT })
    }
    @Test fun homophonesHaveIndependentGlosses() {
        val result = engine().lookup("tuoxie")
        assertEquals(listOf("拖鞋", "妥协"), result.take(2).map { it.hanzi })
        assertEquals("n. slippers", result.first().gloss)
        assertEquals("v. compromise", result[1].gloss)
    }
    @Test fun composesSentenceOutsideDictionary() {
        val first = engine().lookup("woxuexiyingyu").first()
        assertEquals("我学习英语", first.hanzi)
        assertEquals(13, first.consumed)
    }
    @Test fun prefixSelectionKeepsUnconvertedSuffix() {
        val result = engine().lookup("xuexizzz").first { it.hanzi == "学习" }
        assertEquals(5, result.consumed)
    }
    @Test fun initialsAndPartialPinyinWork() {
        assertTrue(engine().lookup("nh").any { it.hanzi == "你好" })
        assertTrue(engine().lookup("tuox").any { it.hanzi == "拖鞋" })
    }
    @Test fun vAndUmlautNormalizeWithoutChangingCandidate() {
        assertEquals("绿色", engine().lookup("lvse").first().hanzi)
        assertEquals("绿色", engine().lookup("lüse").first().hanzi)
    }
    @Test fun learnedWordCanMoveAboveDefaultHomophone() {
        assertEquals("妥协", engine(1).lookup("tuoxie").first().hanzi)
    }
    @Test fun invalidAndOversizedInputDoNotGenerateSentences() {
        assertTrue(engine().lookup("").isEmpty())
        assertTrue(engine().lookup("a".repeat(65)).isEmpty())
        assertTrue(engine().lookup("abc123").isEmpty())
    }
    @Test fun precedingContextChangesHomophoneOrder() {
        assertEquals("时间", engine().lookup("shijian").first().hanzi)
        assertEquals("实践", engine().lookup("shijian", "参加").first().hanzi)
    }
    @Test fun bigramAlsoAffectsWholeSentencePaths() {
        assertEquals("参加实践", engine().lookup("canjiashijian").first().hanzi)
    }
    @Test fun correctsTranspositionInsertionDeletionAndNeighborKey() {
        for ((input, word) in listOf("tuoxei" to "拖鞋", "xuexii" to "学习", "xuex" to "学习", "nihap" to "你好")) {
            assertTrue(input, engine().lookup(input).any { it.hanzi == word && it.correctedFrom == input && it.consumed == input.length })
        }
    }
    @Test fun validExactReadingsAreNeverMarkedAsCorrections() {
        assertTrue(engine().lookup("tuoxie").filter { it.consumed == 6 }.all { it.correctedFrom == null })
    }
    @Test fun correctionCanBeDisabled() {
        assertTrue(engine().lookup("tuoxei", correction = false).none { it.correctedFrom != null })
    }
    @Test fun privateEditorsDoNotUseStoredLearningOrContext() {
        assertEquals("拖鞋", engine(1).lookup("tuoxie", personalized = false).first().hanzi)
        assertEquals("时间", engine().lookup("shijian", "参加", personalized = false).first().hanzi)
    }
}
