package com.example.markdown_editor.data.model

data class FrontMatter(
    val fields: Map<String, FrontMatterValue> = emptyMap(),
) {
    val createdAt: String? get() = (fields["createdAt"] as? FrontMatterValue.Scalar)?.value
    val tags: List<String>
        get() = (fields["tags"] as? FrontMatterValue.StringList)?.values ?: emptyList()

    operator fun get(key: String): FrontMatterValue? = fields[key]

    fun toTagString(): String = tags.joinToString(" ")

    fun toCreatedAtMillis(): Long? = createdAt?.let { value ->
        try {
            java.time.Instant.parse(value).toEpochMilli()
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        val Empty = FrontMatter()

        fun parse(text: String): FrontMatter {
            if (text.isBlank()) return Empty

            val fields = mutableMapOf<String, FrontMatterValue>()
            val lines = text.trim().lines()
            var i = 0

            while (i < lines.size) {
                val line = lines[i].trim()
                if (line.isEmpty() || line.startsWith("#")) {
                    i++; continue
                }

                val colonIdx = line.indexOf(':')
                if (colonIdx < 0) throw IllegalArgumentException("Invalid front matter line: $line")

                val key = line.substring(0, colonIdx).trim()
                val valuePart = line.substring(colonIdx + 1).trim()

                if (valuePart.isEmpty()) {
                    val list = mutableListOf<String>()
                    i++
                    while (i < lines.size && lines[i].trimStart().startsWith("-")) {
                        list.add(lines[i].trim().removePrefix("-").trim())
                        i++
                    }
                    fields[key] = FrontMatterValue.StringList(list)
                } else {
                    fields[key] = FrontMatterValue.Scalar(valuePart)
                    i++
                }
            }

            return FrontMatter(fields)
        }

        fun splitFromContent(content: String): Pair<FrontMatter, String> {
            if (!content.trimStart().startsWith("---")) return Empty to content
            val lines = content.lines()
            val closeIdx = lines.drop(1).indexOfFirst { it.trim() == "---" }
            if (closeIdx < 0) return Empty to content
            val fmText = lines.drop(1).take(closeIdx).joinToString("\n")
            val body = lines.drop(closeIdx + 2).joinToString("\n")
            return parse(fmText) to body
        }
    }

    override fun toString(): String = buildString {
        append("---\n")
        fields.forEach { (key, value) ->
            when (value) {
                is FrontMatterValue.Scalar -> append("$key: ${value.value}\n")
                is FrontMatterValue.StringList -> {
                    append("$key:\n")
                    value.values.forEach { item -> append("- $item\n") }
                }
            }
        }
        append("---")
    }
}

sealed interface FrontMatterValue {
    data class Scalar(val value: String) : FrontMatterValue
    data class StringList(val values: List<String>) : FrontMatterValue
}