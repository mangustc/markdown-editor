package com.example.markdown_editor.domain.editor

sealed class EditorRope {
    abstract val length: Int
    abstract override fun toString(): String

    data class Leaf(val text: String) : EditorRope() {
        override val length = text.length
        override fun toString() = text
    }

    data class Node(val left: EditorRope, val right: EditorRope) : EditorRope() {
        override val length = left.length + right.length
        override fun toString() = left.toString() + right.toString()
    }

    fun insert(pos: Int, s: String): EditorRope = insert(this, pos, Leaf(s))
    fun delete(start: Int, end: Int): EditorRope = delete(this, start, end)
    fun substring(start: Int, end: Int): String = toString().substring(start, end)

    companion object {
        fun of(text: String): EditorRope = split(Leaf(text))

        // Split leaf into balanced tree if large
        private fun split(leaf: Leaf, threshold: Int = 512): EditorRope {
            if (leaf.length <= threshold) return leaf
            val mid = leaf.length / 2
            return Node(
                split(Leaf(leaf.text.substring(0, mid))),
                split(Leaf(leaf.text.substring(mid))),
            )
        }

        private fun insert(rope: EditorRope, pos: Int, ins: EditorRope): EditorRope = when (rope) {
            is Leaf -> {
                val t = rope.text
                Node(Leaf(t.substring(0, pos)), Node(ins, Leaf(t.substring(pos))))
            }

            is Node -> {
                val l = rope.left
                if (pos <= l.length) Node(insert(l, pos, ins), rope.right)
                else Node(l, insert(rope.right, pos - l.length, ins))
            }
        }

        private fun delete(rope: EditorRope, start: Int, end: Int): EditorRope {
            val s = rope.toString()
            return Leaf(s.removeRange(start, end)) // rebalance lazily on next edit
        }
    }
}