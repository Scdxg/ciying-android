package app.ciying.keyboard

/** InputType/EditorInfo bit masks, kept platform-free for JVM tests. */
data class EditorPolicy(val english: Boolean, val numeric: Boolean, val privateInput: Boolean) {
    companion object {
        fun from(type: Int, options: Int): EditorPolicy {
            val kind = type and 0x0f
            val variation = type and 0x0ff0
            val password = (kind == 1 && variation in setOf(0x80, 0x90, 0xe0)) || (kind == 2 && variation == 0x10)
            val direct = kind == 1 && variation in setOf(0x10, 0x20, 0xd0)
            return EditorPolicy(password || direct, kind in setOf(2, 3, 4), password || options and 0x01000000 != 0)
        }
    }
}
