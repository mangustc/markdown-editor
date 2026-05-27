package com.example.markdown_editor.domain.usecases.settings

import com.example.markdown_editor.data.project.SettingsRepository
import com.example.markdown_editor.domain.models.Project
import com.example.markdown_editor.domain.models.Settings
import com.example.markdown_editor.domain.usecases.UseCase

data class GetSettingsInput(
    val project: Project,
)

class GetSettingsUseCase(
    private val settingsRepository: SettingsRepository,
) : UseCase<GetSettingsInput, Settings> {
    override suspend fun invoke(input: GetSettingsInput): Settings {
        return settingsRepository.getSettings(input.project)
    }
}
