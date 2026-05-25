package com.example.markdown_editor.data.sync

sealed class SyncFileAction {
    data class Upload(val relativePath: String) : SyncFileAction()
    data class Download(val relativePath: String) : SyncFileAction()
    data class DeleteLocal(val relativePath: String) : SyncFileAction()
    data class DeleteRemote(val relativePath: String) : SyncFileAction()

    data class ConflictUpload(val relativePath: String) : SyncFileAction()
    data class NoOp(val relativePath: String) : SyncFileAction()
}