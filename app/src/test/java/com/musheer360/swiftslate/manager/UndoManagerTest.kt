package com.musheer360.swiftslate.manager

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class UndoManagerTest {
    private lateinit var undoManager: UndoManager

    @Before
    fun setUp() {
        undoManager = UndoManager(maxHistoryPerField = 5, maxFields = 10)
    }

    // --- undo ---

    @Test
    fun undo_afterPushState_returnsPreviousText() {
        undoManager.pushState("field1", "original")
        assertEquals("original", undoManager.undo("field1"))
    }

    @Test
    fun undo_multiplePushes_walksBackThroughHistory() {
        undoManager.pushState("field1", "first")
        undoManager.pushState("field1", "second")
        undoManager.pushState("field1", "third")
        assertEquals("third", undoManager.undo("field1"))
        assertEquals("second", undoManager.undo("field1"))
        assertEquals("first", undoManager.undo("field1"))
    }

    @Test
    fun undo_emptyStack_returnsNull() {
        assertNull(undoManager.undo("field1"))
    }

    @Test
    fun undo_afterExhaustingStack_returnsNull() {
        undoManager.pushState("field1", "only")
        undoManager.undo("field1")
        assertNull(undoManager.undo("field1"))
    }

    @Test
    fun undo_stackCapEnforced_dropsOldestEntry() {
        undoManager.pushState("field1", "a")
        undoManager.pushState("field1", "b")
        undoManager.pushState("field1", "c")
        undoManager.pushState("field1", "d")
        undoManager.pushState("field1", "e")
        undoManager.pushState("field1", "f") // pushes out "a"

        val results = mutableListOf<String?>()
        repeat(6) { results.add(undoManager.undo("field1")) }

        assertEquals(listOf("f", "e", "d", "c", "b", null), results)
    }
}
