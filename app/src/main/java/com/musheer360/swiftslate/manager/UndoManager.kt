package com.musheer360.swiftslate.manager

import java.util.ArrayDeque

class UndoManager(
    private val maxHistoryPerField: Int = 5,
    private val maxFields: Int = 10
) {
    private data class FieldHistory(
        val undoStack: ArrayDeque<String> = ArrayDeque(),
        val redoStack: ArrayDeque<String> = ArrayDeque()
    )

    private val fields = object : LinkedHashMap<String, FieldHistory>(maxFields, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, FieldHistory>?): Boolean {
            return size > maxFields
        }
    }

    private fun getOrCreate(fieldId: String): FieldHistory {
        return fields.getOrPut(fieldId) { FieldHistory() }
    }

    /** Record a new transformation. Clears redo since a new transformation invalidates redo history. */
    fun pushState(fieldId: String, text: String) {
        val history = getOrCreate(fieldId)
        history.undoStack.addLast(text)
        if (history.undoStack.size > maxHistoryPerField) {
            history.undoStack.removeFirst()
        }
        history.redoStack.clear()
    }

    fun undo(fieldId: String): String? {
        val history = fields[fieldId] ?: return null
        if (history.undoStack.isEmpty()) return null
        return history.undoStack.removeLast()
    }

    fun pushRedo(fieldId: String, text: String) {
        val history = getOrCreate(fieldId)
        history.redoStack.addLast(text)
    }

    /** Push onto undo without clearing redo. Used during redo to make the current state undoable. */
    fun pushUndo(fieldId: String, text: String) {
        val history = getOrCreate(fieldId)
        history.undoStack.addLast(text)
        if (history.undoStack.size > maxHistoryPerField) {
            history.undoStack.removeFirst()
        }
    }

    fun redo(fieldId: String): String? {
        val history = fields[fieldId] ?: return null
        if (history.redoStack.isEmpty()) return null
        return history.redoStack.removeLast()
    }
}
