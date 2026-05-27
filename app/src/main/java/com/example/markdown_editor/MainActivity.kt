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
import com.example.markdown_editor.domain.models.Attachment
import com.example.markdown_editor.domain.models.FileSystemPath
import com.example.markdown_editor.ui.AppScaffold
import com.example.markdown_editor.ui.MarkdowneditorTheme
import com.example.markdown_editor.ui.viewmodel.AppViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private val appViewModel: AppViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING)

        handleShareIntent(intent, appViewModel)

        setContent {
            MarkdowneditorTheme(darkTheme = false) {
                AppScaffold(appViewModel = appViewModel)
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
            Attachment.PendingAttachment(
                fileSystemPath = FileSystemPath(uri.toString()),
                displayName = displayName,
                type = if (resolvedMime.startsWith("image/")) Attachment.AttachmentType.IMAGE else Attachment.AttachmentType.FILE,
            )
        }

        if (text != null || attachments.isNotEmpty()) {
            viewModel.onShareIntent(text, attachments)
        }
    }
}