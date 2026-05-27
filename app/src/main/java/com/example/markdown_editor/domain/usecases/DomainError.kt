package com.example.markdown_editor.domain.usecases

sealed interface DomainError {
    data class Unexpected(val message: String) : DomainError
}