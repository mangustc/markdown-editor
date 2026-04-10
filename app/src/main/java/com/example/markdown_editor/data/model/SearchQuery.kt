package com.example.markdown_editor.data.model

data class SearchQuery(
    val bodyTerms: List<String>,       // plain words
    val tagFilters: List<String>,      // from  tag:foo
    val propFilters: Map<String, String>, // from  [key:value]
    val nameFilter: String?            // from  name:foo
) {
    val isEmpty: Boolean get() =
        bodyTerms.isEmpty() && tagFilters.isEmpty() &&
                propFilters.isEmpty() && nameFilter == null

    companion object {
        private val TAG_REGEX    = Regex("""tag:(\S+)""")
        private val PROP_REGEX   = Regex("""\[(\w+):([^\]]+)]""")
        private val NAME_REGEX   = Regex("""name:(\S+)""")

        fun parse(raw: String): SearchQuery {
            var remainder = raw

            val tags = TAG_REGEX.findAll(remainder).map { it.groupValues[1] }.toList()
            remainder = TAG_REGEX.replace(remainder, "")

            val props = PROP_REGEX.findAll(remainder)
                .associate { it.groupValues[1] to it.groupValues[2] }
            remainder = PROP_REGEX.replace(remainder, "")

            val nameMatch = NAME_REGEX.find(remainder)
            val name = nameMatch?.groupValues?.get(1)
            remainder = NAME_REGEX.replace(remainder, "")

            val bodyTerms = remainder.trim().split(Regex("""\s+""")).filter { it.isNotBlank() }

            return SearchQuery(bodyTerms, tags, props, name)
        }
    }
}