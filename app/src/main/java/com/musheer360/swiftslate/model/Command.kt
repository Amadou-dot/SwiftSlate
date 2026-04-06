package com.musheer360.swiftslate.model

enum class CommandType {
    AI, TEXT_REPLACER
}

data class Command(
    val trigger: String,
    val prompt: String,
    val isBuiltIn: Boolean = false,
    val type: CommandType = CommandType.AI
)
