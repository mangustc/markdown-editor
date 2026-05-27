package com.example.markdown_editor.domain.usecases.settings

import com.example.markdown_editor.data.project.SettingsRepository
import com.example.markdown_editor.domain.models.Project
import com.example.markdown_editor.domain.models.Settings
import com.example.markdown_editor.domain.usecases.UseCase

data class SetSettingsInput(
    val project: Project,
    val newSettings: Settings,
)

class SetSettingsUseCase(
    private val settingsRepository: SettingsRepository,
) : UseCase<SetSettingsInput, Settings> {
    override suspend fun invoke(input: SetSettingsInput): Settings {
        return settingsRepository.setSettings(input.project, input.newSettings)
    }
}
