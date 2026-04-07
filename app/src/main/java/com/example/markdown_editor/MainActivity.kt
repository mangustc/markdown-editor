package com.example.markdown_editor

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan
import android.text.style.ImageSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.util.AttributeSet
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.widget.AppCompatEditText
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.markdown_editor.ui.theme.MarkdowneditorTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.text.TextWatcher
import android.widget.TextView

data class SpanInfo(val start: Int, val end: Int, val type: TokenType)
enum class TokenType { H1, H2, H3, BOLD, ITALIC, CODE_INLINE, CODE_BLOCK, IMAGE }
object MarkdownParser {
    private val rules = listOf(
        TokenType.H1          to Regex("^# .+", RegexOption.MULTILINE),
        TokenType.H2          to Regex("^## .+", RegexOption.MULTILINE),
        TokenType.H3          to Regex("^### .+", RegexOption.MULTILINE),
        TokenType.BOLD        to Regex("\\*\\*.+?\\*\\*"),
        TokenType.ITALIC      to Regex("(?<!\\*)\\*(?!\\*).+?(?<!\\*)\\*(?!\\*)"),
        TokenType.CODE_INLINE to Regex("`[^`]+`"),
        TokenType.CODE_BLOCK  to Regex("```[\\s\\S]+?```"),
        TokenType.IMAGE       to Regex("!\\[.*?]\\((.+?)\\)"),
    )

    fun parse(text: String): List<SpanInfo> =
        rules.flatMap { (type, regex) ->
            regex.findAll(text).map { SpanInfo(it.range.first, it.range.last + 1, type) }
        }
}

object SpanRenderer {
    fun apply(editable: Editable, spans: List<SpanInfo>) {
        // Remove all old Markdown spans
        editable.getSpans(0, editable.length, Any::class.java)
            .filter { it is StyleSpan || it is RelativeSizeSpan ||
                    it is ForegroundColorSpan || it is TypefaceSpan ||
                    it is ImageSpan || it is BackgroundColorSpan
            }
            .forEach { editable.removeSpan(it) }

        spans.forEach { info ->
            if (info.start >= info.end || info.end > editable.length) return@forEach
            setSpansFor(editable, info)
        }
    }

    private fun setSpansFor(e: Editable, info: SpanInfo) {
        val s = info.start; val end = info.end
        val F = Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        when (info.type) {
            TokenType.H1 -> {
                e.setSpan(StyleSpan(Typeface.BOLD), s, end, F)
                e.setSpan(RelativeSizeSpan(2.0f), s, end, F)
                e.setSpan(ForegroundColorSpan(Color.parseColor("#1A73E8")), s, end, F)
            }
            TokenType.H2 -> {
                e.setSpan(StyleSpan(Typeface.BOLD), s, end, F)
                e.setSpan(RelativeSizeSpan(1.5f), s, end, F)
            }
            TokenType.H3 -> {
                e.setSpan(StyleSpan(Typeface.BOLD), s, end, F)
                e.setSpan(RelativeSizeSpan(1.2f), s, end, F)
            }
            TokenType.BOLD ->
                e.setSpan(StyleSpan(Typeface.BOLD), s, end, F)
            TokenType.ITALIC ->
                e.setSpan(StyleSpan(Typeface.ITALIC), s, end, F)
            TokenType.CODE_INLINE -> {
                e.setSpan(TypefaceSpan("monospace"), s, end, F)
                e.setSpan(BackgroundColorSpan(Color.parseColor("#F5F5F5")), s, end, F)
                e.setSpan(ForegroundColorSpan(Color.parseColor("#C7254E")), s, end, F)
            }
            TokenType.CODE_BLOCK -> {
                e.setSpan(TypefaceSpan("monospace"), s, end, F)
                e.setSpan(BackgroundColorSpan(Color.parseColor("#F8F8F8")), s, end, F)
            }
            else -> Unit // IMAGE handled separately below
        }
    }
}

class MarkdownEditText @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : AppCompatEditText(context, attrs) {

    private var parseJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    init {
        addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                s ?: return
                parseJob?.cancel()
                parseJob = scope.launch {
                    delay(120)
                    val spans = withContext(Dispatchers.Default) {
                        MarkdownParser.parse(s.toString())
                    }
                    SpanRenderer.apply(s, spans)
                }
            }
        })
    }
}
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MarkdowneditorTheme(
                darkTheme = false,
            ) {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AndroidView(
                        modifier = Modifier.padding(innerPadding),
                        factory = { context ->
                            val editText = MarkdownEditText(context)
                            editText.setText("Type or write markdown here!\n# Example Heading\n*Italic text*\n`Inline code`\n\n```\nCode block\n```",
                                TextView.BufferType.SPANNABLE)
                            editText.isFocusable = true
                            editText.isFocusableInTouchMode = true
                            editText.requestFocus()
                            editText
                        }
                    )
                }
            }
        }
    }
}