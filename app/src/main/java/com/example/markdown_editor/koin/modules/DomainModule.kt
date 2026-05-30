package com.example.markdown_editor.koin.modules

import com.example.markdown_editor.domain.usecases.editor.ApplyEditorEventUseCase
import com.example.markdown_editor.domain.usecases.editor.GetRealSpanInfoLinkTypeUseCase
import com.example.markdown_editor.domain.usecases.linkPreview.GetLinkPreviewUseCase
import com.example.markdown_editor.domain.usecases.messenger.GetMessagesUseCase
import com.example.markdown_editor.domain.usecases.messenger.GetPinnedMessagesUseCase
import com.example.markdown_editor.domain.usecases.messenger.SendNoteUseCase
import com.example.markdown_editor.domain.usecases.notes.CreateNoteUseCase
import com.example.markdown_editor.domain.usecases.notes.DeleteNoteUseCase
import com.example.markdown_editor.domain.usecases.notes.GetNoteUseCase
import com.example.markdown_editor.domain.usecases.notes.RenameNoteUseCase
import com.example.markdown_editor.domain.usecases.notes.SaveNoteTextUseCase
import com.example.markdown_editor.domain.usecases.notes.ToggleNoteTagUseCase
import com.example.markdown_editor.domain.usecases.project.CopyToAssetsUseCase
import com.example.markdown_editor.domain.usecases.project.GetAllTagsUseCase
import com.example.markdown_editor.domain.usecases.project.GetNotesUseCase
import com.example.markdown_editor.domain.usecases.project.GetProjectFileUseCase
import com.example.markdown_editor.domain.usecases.project.LoadSavedProjectUseCase
import com.example.markdown_editor.domain.usecases.project.SelectProjectUseCase
import com.example.markdown_editor.domain.usecases.project.SyncDatabaseUseCase
import com.example.markdown_editor.domain.usecases.search.ApplySearchEventUseCase
import com.example.markdown_editor.domain.usecases.settings.GetSettingsUseCase
import com.example.markdown_editor.domain.usecases.settings.SetSettingsUseCase
import com.example.markdown_editor.domain.usecases.sync.SyncProjectUseCase
import org.koin.dsl.module
import org.koin.plugin.module.dsl.factory

val domainModule = module {
    factory<SyncProjectUseCase>()
    factory<GetMessagesUseCase>()
    factory<GetPinnedMessagesUseCase>()
    factory<LoadSavedProjectUseCase>()
    factory<SelectProjectUseCase>()
    factory<SetSettingsUseCase>()
    factory<GetSettingsUseCase>()
    factory<SyncDatabaseUseCase>()
    factory<GetNotesUseCase>()
    factory<CreateNoteUseCase>()
    factory<GetNoteUseCase>()
    factory<DeleteNoteUseCase>()
    factory<RenameNoteUseCase>()
    factory<ToggleNoteTagUseCase>()
    factory<GetAllTagsUseCase>()
    factory<GetLinkPreviewUseCase>()
    factory<CopyToAssetsUseCase>()
    factory<GetProjectFileUseCase>()
    factory<GetRealSpanInfoLinkTypeUseCase>()
    factory<SaveNoteTextUseCase>()
    factory<SendNoteUseCase>()
    factory<ApplyEditorEventUseCase>()
    factory<ApplySearchEventUseCase>()
}
