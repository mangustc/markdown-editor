package com.example.markdown_editor.domain.models

import java.time.Instant

data class FrontMatter(
    val fields: Map<String, FrontMatterValue> = emptyMap(),
) {
    sealed interface FrontMatterValue {
        data class Scalar(val value: String) : FrontMatterValue
        data class StringList(val values: List<String>) : FrontMatterValue
    }

    val createdAt: String? get() = (fields[CREATED_AT_FIELD] as? FrontMatterValue.Scalar)?.value
    val tags: List<String>
        get() = (fields[TAGS_FIELD] as? FrontMatterValue.StringList)?.values ?: emptyList()

    operator fun get(key: String): FrontMatterValue? = fields[key]

    fun toTagString(): String = tags.joinToString(" ")

    fun toCreatedAtMillis(): Long? = createdAt?.let { value ->
        try {
            Instant.parse(value).toEpochMilli()
        } catch (e: Exception) {
            null
        }
    }

    fun withField(key: String, value: FrontMatterValue): FrontMatter {
        val newFields = fields.toMutableMap()
        newFields[key] = value
        return copy(fields = newFields)
    }

    fun withRenamedKey(oldKey: String, newKey: String): FrontMatter {
        if (oldKey == newKey || newKey.isBlank()) return this
        val newFields = mutableMapOf<String, FrontMatterValue>()
        for ((k, v) in fields) {
            if (k == oldKey) newFields[newKey] = v else newFields[k] = v
        }
        return copy(fields = newFields)
    }

    fun withTag(tag: String): FrontMatter {
        if (tag.isBlank()) return this
        val currentTags = tags.toMutableList()
        if (!currentTags.contains(tag)) currentTags.add(tag)
        return withField(TAGS_FIELD, FrontMatterValue.StringList(currentTags))
    }

    fun withoutTag(tag: String): FrontMatter {
        val currentTags = tags.toMutableList()
        currentTags.remove(tag)
        return withField(TAGS_FIELD, FrontMatterValue.StringList(currentTags))
    }

    fun withoutField(key: String): FrontMatter {
        if (key == TAGS_FIELD || key == CREATED_AT_FIELD) return this

        val newFields = fields.toMutableMap()
        newFields.remove(key)
        return copy(fields = newFields)
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

    companion object {
        const val PINNED_TAG = "pinned"
        const val QUICK_NOTE_TAG = "quick-note"

        const val TAGS_FIELD = "tags"
        const val CREATED_AT_FIELD = "createdAt"

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
            val frontMatter = try {
                parse(fmText)
            } catch (e: IllegalArgumentException) {
                Empty
            }
            return frontMatter to body
        }
    }
}
