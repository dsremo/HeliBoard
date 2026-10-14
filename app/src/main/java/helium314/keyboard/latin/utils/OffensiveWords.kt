package helium314.keyboard.latin.utils

import android.content.Context
import java.util.Locale

object OffensiveWords {

    private const val ASSET_NAME = "offensive_words.txt"

    @Volatile
    private var words: Set<String> = emptySet()

    fun init(context: Context) {
        if (words.isNotEmpty()) return
        synchronized(this) {
            if (words.isNotEmpty()) return
            words = runCatching {
                context.assets.open(ASSET_NAME).bufferedReader().useLines { lines ->
                    lines.map { it.trim().lowercase(Locale.ROOT) }.filter { it.isNotEmpty() }.toSet()
                }
            }.getOrDefault(emptySet())
        }
    }

    fun isOffensive(word: String): Boolean =
        words.isNotEmpty() && words.contains(word.lowercase(Locale.ROOT))
}
