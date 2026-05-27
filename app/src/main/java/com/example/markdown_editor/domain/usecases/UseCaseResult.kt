package com.example.markdown_editor.domain.usecases

sealed interface UseCaseResult<out T> {
    data class Success<T>(val data: T) : UseCaseResult<T>
    data class Failure(val error: DomainError) : UseCaseResult<Nothing>
}