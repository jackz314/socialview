package com.hendraanggrian.widget

import android.content.Context
import android.support.v7.widget.AppCompatMultiAutoCompleteTextView
import android.text.Editable
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils.copySpansFrom
import android.text.TextWatcher
import android.util.AttributeSet
import android.widget.ArrayAdapter
import android.widget.MultiAutoCompleteTextView
import com.hendraanggrian.widget.internal.SocialViewImpl

/**
 * [android.widget.MultiAutoCompleteTextView] with hashtag, mention, and hyperlink support.
 *
 * @see SocialView
 */
class SocialAutoCompleteTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.support.v7.appcompat.R.attr.autoCompleteTextViewStyle
) : AppCompatMultiAutoCompleteTextView(context, attrs, defStyleAttr),
    SocialView<MultiAutoCompleteTextView> by SocialViewImpl() {

    private val textWatcher: TextWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
        override fun afterTextChanged(editable: Editable?) {}
        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
            if (s.isNotEmpty() && start < s.length) when (s[start]) {
                '#' -> if (adapter !== hashtagAdapter) setAdapter(hashtagAdapter)
                '@' -> if (adapter !== mentionAdapter) setAdapter(mentionAdapter)
            }
        }
    }
    private val enabledSymbols = mutableSetOf<Char>()

    var hashtagAdapter: ArrayAdapter<*>? = null
    var mentionAdapter: ArrayAdapter<*>? = null

    init {
        initialize(this, attrs)
        addTextChangedListener(textWatcher)
        if (isHashtagEnabled()) enabledSymbols += '#'
        if (isMentionEnabled()) enabledSymbols += '@'
        setTokenizer(SymbolsTokenizer(enabledSymbols))
    }

    override fun setHashtagEnabled(enabled: Boolean) {
        super.setHashtagEnabled(enabled)
        enableSymbol('#', enabled)
        setTokenizer(SymbolsTokenizer(enabledSymbols))
    }

    override fun setMentionEnabled(enabled: Boolean) {
        super.setMentionEnabled(enabled)
        enableSymbol('@', enabled)
        setTokenizer(SymbolsTokenizer(enabledSymbols))
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun enableSymbol(symbol: Char, enable: Boolean) = when {
        enable -> enabledSymbols += symbol
        else -> enabledSymbols -= symbol
    }

    /**
     * While [MultiAutoCompleteTextView.CommaTokenizer] tracks only comma symbol,
     * [SymbolsTokenizer] can track multiple characters, in this instance,
     * are hashtag and at symbol.
     */
    class SymbolsTokenizer(private val symbols: Set<Char>) : MultiAutoCompleteTextView.Tokenizer {
        override fun findTokenStart(text: CharSequence, cursor: Int): Int {
            var i = cursor
            while (i > 0 && !symbols.contains(text[i - 1])) i--
            while (i < cursor && text[i] == ' ') i++
            return i
        }

        override fun findTokenEnd(text: CharSequence, cursor: Int): Int {
            var i = cursor
            val len = text.length
            while (i < len) if (symbols.contains(text[i])) return i else i++
            return len
        }

        override fun terminateToken(text: CharSequence): CharSequence {
            var i = text.length
            while (i > 0 && text[i - 1] == ' ') i--
            return when {
                i > 0 && symbols.contains(text[i - 1]) -> text
                text is Spanned -> {
                    val sp = SpannableString("$text ")
                    copySpansFrom(text, 0, text.length, Any::class.java, sp, 0)
                    sp
                }
                else -> "$text "
            }
        }
    }
}