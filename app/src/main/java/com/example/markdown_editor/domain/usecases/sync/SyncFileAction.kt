package com.example.markdown_editor.domain.usecases.sync

import com.example.markdown_editor.domain.models.RelativePath

sealed class SyncFileAction {
    data class Upload(val relativePath: RelativePath) : SyncFileAction()
    data class Download(val relativePath: RelativePath) : SyncFileAction()
    data class DeleteLocal(val relativePath: RelativePath) : SyncFileAction()
    data class DeleteRemote(val relativePath: RelativePath) : SyncFileAction()
    data class ConflictUpload(val relativePath: RelativePath) : SyncFileAction()
    data class NoOp(val relativePath: RelativePath) : SyncFileAction()
}