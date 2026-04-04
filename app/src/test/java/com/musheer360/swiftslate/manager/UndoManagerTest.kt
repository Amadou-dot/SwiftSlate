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

    // --- redo ---

    @Test
    fun redo_afterUndoAndPushRedo_restoresUndoneText() {
        undoManager.pushState("field1", "original")
        val undone = undoManager.undo("field1")
        undoManager.pushRedo("field1", "transformed")
        assertEquals("transformed", undoManager.redo("field1"))
    }

    @Test
    fun redo_emptyStack_returnsNull() {
        assertNull(undoManager.redo("field1"))
    }

    @Test
    fun redo_fullUndoRedoCycle_preservesAllStates() {
        undoManager.pushState("field1", "v1")
        undoManager.pushState("field1", "v2")

        // Undo twice: v2 -> v1
        val u1 = undoManager.undo("field1")
        undoManager.pushRedo("field1", "v3") // current text before undo
        assertEquals("v2", u1)

        val u2 = undoManager.undo("field1")
        undoManager.pushRedo("field1", "v2")
        assertEquals("v1", u2)

        // Redo twice: v1 -> v2 -> v3
        val r1 = undoManager.redo("field1")
        undoManager.pushUndo("field1", "v1") // current text before redo
        assertEquals("v2", r1)

        val r2 = undoManager.redo("field1")
        undoManager.pushUndo("field1", "v2")
        assertEquals("v3", r2)
    }

    @Test
    fun redo_clearedOnNewPushState() {
        undoManager.pushState("field1", "original")
        undoManager.undo("field1")
        undoManager.pushRedo("field1", "transformed")

        undoManager.pushState("field1", "new transformation")
        assertNull(undoManager.redo("field1"))
    }

    @Test
    fun pushUndo_doesNotClearRedo() {
        undoManager.pushState("field1", "v1")
        undoManager.undo("field1")
        undoManager.pushRedo("field1", "v2")

        undoManager.pushUndo("field1", "v1") // simulate redo caller saving state
        assertNotNull(undoManager.redo("field1")) // redo stack should still have v2
    }

    // --- LRU eviction ---

    @Test
    fun lruEviction_dropsOldestField() {
        val manager = UndoManager(maxHistoryPerField = 5, maxFields = 3)
        manager.pushState("field1", "a")
        manager.pushState("field2", "b")
        manager.pushState("field3", "c")
        manager.pushState("field4", "d") // evicts field1

        assertNull(manager.undo("field1"))
        assertEquals("b", manager.undo("field2"))
    }

    @Test
    fun lruEviction_accessRefreshesOrder() {
        val manager = UndoManager(maxHistoryPerField = 5, maxFields = 3)
        manager.pushState("field1", "a")
        manager.pushState("field2", "b")
        manager.pushState("field3", "c")

        // Access field1 to refresh it in LRU
        manager.undo("field1")
        manager.pushState("field1", "a-refreshed")

        manager.pushState("field4", "d") // evicts field2 (now oldest)

        assertEquals("a-refreshed", manager.undo("field1"))
        assertNull(manager.undo("field2"))
    }

    // --- per-field isolation ---

    @Test
    fun perFieldIsolation_undoDoesNotAffectOtherFields() {
        undoManager.pushState("field1", "text1")
        undoManager.pushState("field2", "text2")

        assertEquals("text1", undoManager.undo("field1"))
        assertEquals("text2", undoManager.undo("field2"))
    }
}
