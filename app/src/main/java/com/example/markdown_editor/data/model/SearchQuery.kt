package com.example.markdown_editor.data.model

data class SearchQuery(
    val bodyTerms: List<String> = emptyList(),
    val negatedBodyTerms: List<String> = emptyList(),
    val tagFilters: List<String> = emptyList(),
    val negatedTagFilters: List<String> = emptyList(),
    val propFilters: Map<String, String> = emptyMap(),
    val nameFilter: String? = null,
    val negatedNameFilter: String? = null,
) {
    val isEmpty: Boolean
        get() =
            bodyTerms.isEmpty() && negatedBodyTerms.isEmpty() &&
                    tagFilters.isEmpty() && negatedTagFilters.isEmpty() &&
                    propFilters.isEmpty() && nameFilter == null && negatedNameFilter == null

    fun buildFtsMatchQuery(): String? {
        if (bodyTerms.isEmpty()) return null
        return bodyTerms.joinToString(" AND ") { "${sanitizeFtsTerm(it)}*" }
    }

    fun positiveTagLikes(): List<String> = tagFilters
    fun negatedTagLikes(): List<String> = negatedTagFilters

    companion object {
        private val NEG_TAG_REGEX = Regex("""-tag:(\S+)""")
        private val TAG_REGEX = Regex("""(?<!-)tag:(\S+)""")
        private val PROP_REGEX = Regex("""\[(\w+):([^\]]+)]""")
        private val NEG_NAME_REGEX = Regex("""-name:(\S+)""")
        private val NAME_REGEX = Regex("""(?<!-)name:(\S+)""")

        fun parse(raw: String): SearchQuery {
            var remainder = raw

            val negatedTags = NEG_TAG_REGEX.findAll(remainder).map { it.groupValues[1] }.toList()
            remainder = NEG_TAG_REGEX.replace(remainder, "")

            val tags = TAG_REGEX.findAll(remainder).map { it.groupValues[1] }.toList()
            remainder = TAG_REGEX.replace(remainder, "")

            val props = PROP_REGEX.findAll(remainder)
                .associate { it.groupValues[1] to it.groupValues[2] }
            remainder = PROP_REGEX.replace(remainder, "")

            val negatedNameMatch = NEG_NAME_REGEX.find(remainder)
            val negatedName = negatedNameMatch?.groupValues?.get(1)
            remainder = NEG_NAME_REGEX.replace(remainder, "")

            val nameMatch = NAME_REGEX.find(remainder)
            val name = nameMatch?.groupValues?.get(1)
            remainder = NAME_REGEX.replace(remainder, "")

            val allTokens = remainder.trim().split(Regex("""\s+""")).filter { it.isNotBlank() }
            val bodyTerms = allTokens.filter { !it.startsWith("-") }
            val negatedBodyTerms =
                allTokens.filter { it.startsWith("-") }.map { it.removePrefix("-") }

            return SearchQuery(
                bodyTerms = bodyTerms,
                negatedBodyTerms = negatedBodyTerms,
                tagFilters = tags,
                negatedTagFilters = negatedTags,
                propFilters = props,
                nameFilter = name,
                negatedNameFilter = negatedName,
            )
        }

        fun sanitizeFtsTerm(term: String): String =
            term.replace(Regex("""[\"'*\-^]"""), "")
    }
}