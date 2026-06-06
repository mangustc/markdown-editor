package com.example.markdown_editor.domain.usecases.editor

import com.example.markdown_editor.domain.models.FileSystemPath
import com.example.markdown_editor.domain.models.Project
import com.example.markdown_editor.domain.models.TextState
import com.example.markdown_editor.domain.usecases.UseCase
import com.example.markdown_editor.domain.usecases.project.CopyToAssetsInput
import com.example.markdown_editor.domain.usecases.project.CopyToAssetsUseCase

data class ApplyEditorEventInput(
    val project: Project,
    val state: TextState,
    val event: EditorEvent,
)

class ApplyEditorEventUseCase(
    private val copyToAssetsUseCase: CopyToAssetsUseCase,
) : UseCase<ApplyEditorEventInput, Unit> {
    override suspend fun invoke(input: ApplyEditorEventInput) {
        val project = input.project
        val event = input.event
        val state = input.state
        when (event) {
            is EditorEvent.AttachPhoto -> {
                val projectFile =
                    copyToAssetsUseCase(
                        CopyToAssetsInput(
                            project = project,
                            assetPath = event.path,
                        ),
                    )
                state.insertLink(
                    label = projectFile.relativePath.basename,
                    payload = projectFile.relativePath.value,
                    isImage = true,
                )
            }

            is EditorEvent.AttachFile -> {
                val projectFile =
                    copyToAssetsUseCase(
                        CopyToAssetsInput(
                            project = project,
                            assetPath = event.path,
                        ),
                    )
                state.insertLink(
                    label = event.displayName ?: projectFile.relativePath.basename,
                    payload = projectFile.relativePath.value,
                    isImage = false,
                )
            }

            is EditorEvent.Undo -> {
                state.undoState.undo()
            }

            is EditorEvent.Redo -> {
                state.undoState.redo()
            }

            is EditorEvent.Bold -> {
                state.edit {
                    bold()
                }
            }

            is EditorEvent.Code -> {
                state.edit {
                    code()
                }
            }

            is EditorEvent.Italic -> {
                state.edit {
                    italic()
                }
            }

            is EditorEvent.InsertNoteLink -> {
                state.insertLink(
                    label = event.note.name,
                    payload = event.note.projectFile.relativePath.value,
                )
            }
        }
    }
}
