package com.example.markdown_editor.domain.usecases

interface UseCase<in Input, out Output> {
    suspend operator fun invoke(input: Input): Output
}