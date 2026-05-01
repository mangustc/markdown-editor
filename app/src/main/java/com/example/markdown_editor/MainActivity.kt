package com.example.markdown_editor

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModelProvider
import com.example.markdown_editor.ui.AppScaffold
import com.example.markdown_editor.ui.MarkdowneditorTheme
import com.example.markdown_editor.ui.messenger.Attachment
import com.example.markdown_editor.ui.messenger.AttachmentType
import com.example.markdown_editor.ui.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)

        val appViewModel = ViewModelProvider(this)[AppViewModel::class.java]
        handleShareIntent(intent, appViewModel)

        setContent {
            MarkdowneditorTheme(darkTheme = false) {
                AppScaffold()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        val appViewModel = ViewModelProvider(this)[AppViewModel::class.java]
        handleShareIntent(intent, appViewModel)
    }

    private fun handleShareIntent(intent: Intent, viewModel: AppViewModel) {
        val action = intent.action ?: return
        if (action != Intent.ACTION_SEND && action != Intent.ACTION_SEND_MULTIPLE) return

        val text = intent.getStringExtra(Intent.EXTRA_TEXT)
        val mimeType = intent.type ?: ""

        val uris: List<Uri> = when (action) {
            Intent.ACTION_SEND -> {
                val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
                listOfNotNull(uri)
            }

            Intent.ACTION_SEND_MULTIPLE -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                        ?: emptyList()
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM) ?: emptyList()
                }
            }

            else -> emptyList()
        }

        val attachments = uris.map { uri ->
            val resolvedMime = contentResolver.getType(uri) ?: mimeType
            val displayName = DocumentFile.fromSingleUri(this, uri)?.name ?: "File"
            Attachment(
                uri = uri,
                displayName = displayName,
                type = if (resolvedMime.startsWith("image/")) AttachmentType.PENDING_IMAGE else AttachmentType.PENDING_FILE,
            )
        }

        if (text != null || attachments.isNotEmpty()) {
            viewModel.onShareIntent(text, attachments)
        }
    }
}