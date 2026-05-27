package com.example.markdown_editor.domain.usecases

import kotlinx.coroutines.flow.Flow

interface FlowUseCase<in Input, out Output> {
    operator fun invoke(input: Input): Flow<Output>
}