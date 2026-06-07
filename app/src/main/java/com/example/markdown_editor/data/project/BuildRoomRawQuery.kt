package com.example.markdown_editor.data.project

import androidx.room.RoomRawQuery
import com.example.markdown_editor.domain.models.Project
import com.example.markdown_editor.domain.models.SearchQuery

fun SearchQuery.buildRoomRawQuery(project: Project): RoomRawQuery {
    val args = mutableListOf<Any>()
    val sb = StringBuilder()
    val hasFts = bodyTerms.isNotEmpty()
    val ftsMatchExpr = buildFtsMatchQuery()

    sb.append("SELECT notes.* FROM notes")
    if (hasFts && ftsMatchExpr != null) {
        sb.append("\nJOIN notesFts ON notes.rowid = notesFts.rowid")
        sb.append("\n  AND notesFts MATCH ?")
        args.add(ftsMatchExpr)
    }

    val conditions = mutableListOf<String>()
    conditions.add("notes.projectId = (SELECT id FROM projects WHERE rootPath = ? LIMIT 1)")
    args.add(project.rootFileSystemPath.value)
    for (term in negatedBodyTerms) {
        conditions.add("notes.body NOT LIKE ?")
        args.add("%$term%")
    }
    for (tag in positiveTagLikes()) {
        conditions.add("notes.tags LIKE ?")
        args.add("%$tag%")
    }
    for (tag in negatedTagLikes()) {
        conditions.add("notes.tags NOT LIKE ?")
        args.add("%$tag%")
    }
    nameFilter?.let {
        conditions.add("notes.name LIKE ?")
        args.add("%$it%")
    }
    negatedNameFilter?.let {
        conditions.add("notes.name NOT LIKE ?")
        args.add("%$it%")
    }
    if (conditions.isNotEmpty()) {
        sb.append("\nWHERE ")
        sb.append(conditions.joinToString("\n  AND "))
    }

    val pinnedClause = if (pinnedFirst)
        "CASE WHEN notes.tags LIKE '%pinned%' THEN 0 ELSE 1 END ASC,\n  "
    else ""
    val sortClause = when (sortBy) {
        SearchQuery.SortBy.LAST_MODIFIED -> "notes.lastModified DESC"
        SearchQuery.SortBy.CREATED_AT ->
            "notes.createdAt DESC, notes.lastModified DESC"
    }
    sb.append("\nORDER BY $pinnedClause$sortClause")

    return RoomRawQuery(
        sql = sb.toString(),
        onBindStatement = { statement ->
            args.forEachIndexed { index, arg ->
                when (arg) {
                    is String -> statement.bindText(index + 1, arg)
                    is Long -> statement.bindLong(index + 1, arg)
                    is Int -> statement.bindLong(index + 1, arg.toLong())
                    is Double -> statement.bindDouble(index + 1, arg)
                    is Boolean -> statement.bindBoolean(index + 1, arg)
                    else -> throw IllegalArgumentException("Unknown argument type")
                }
            }
        },
    )
}
