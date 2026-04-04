# Undo History Stack Design

## Summary

Replace the single-level undo toggle in `AssistantService` with a per-field undo/redo history stack, managed by a new `UndoManager` class. Adds a `?redo` built-in command.

## Decisions

- **Undo + redo:** `?undo` walks back, `?redo` walks forward
- **Per-field history:** Keyed by a stable identifier derived from `AccessibilityNodeInfo`
- **Stack depth:** 5 entries per field
- **Field limit:** 10 fields with LRU eviction
- **Persistence:** In-memory only (lost on service restart)

## New File: `UndoManager.kt`

**Location:** `app/src/main/java/com/musheer360/swiftslate/manager/UndoManager.kt`

Manages per-field undo/redo stacks with LRU eviction.

### Field Identification

Derives a key from `AccessibilityNodeInfo`:
- Primary: `"${node.windowId}:${node.viewIdResourceName}"`
- Fallback (when `viewIdResourceName` is null): `"${node.windowId}:${node.className}:${node.hashCode()}"`

**Note:** Node references can go stale when the user navigates away from a field. The field ID is derived at call time from a live node; if the node is stale, the ID may not match the original entry. This is acceptable — stale lookups simply return null (treated as empty stack).

### Data Structure

```
class UndoManager(maxHistoryPerField: Int = 5, maxFields: Int = 10)
```

Internal state per field:
- `undoStack: ArrayDeque<String>` — capped at `maxHistoryPerField`
- `redoStack: ArrayDeque<String>` — cleared on new transformation, max size bounded organically by undo stack size

Field map: `LinkedHashMap` with `accessOrder = true`, evicting the eldest entry when size exceeds `maxFields`.

### Public API

```kotlin
fun fieldId(node: AccessibilityNodeInfo): String
```
Derives the field key from a node.

```kotlin
fun pushState(fieldId: String, text: String)
```
Called before each transformation. Pushes text onto undo stack and clears redo stack. Drops the oldest undo entry if the stack exceeds `maxHistoryPerField`.

```kotlin
fun undo(fieldId: String): String?
```
Pops from undo stack and returns the text. Returns null if stack is empty. Caller must push current field text onto redo via `pushRedo()` before replacing.

```kotlin
fun pushRedo(fieldId: String, text: String)
```
Pushes current text onto redo stack before applying undo result.

```kotlin
fun pushUndo(fieldId: String, text: String)
```
Pushes text onto undo stack *without* clearing redo. Used by redo operations to record the state being left.

```kotlin
fun redo(fieldId: String): String?
```
Pops from redo stack and returns the text. Returns null if stack is empty. Caller must push current field text onto undo via `pushUndo()` before replacing.

## Changes to `AssistantService.kt`

- Remove `lastOriginalText: String?` field
- Add `private val undoManager = UndoManager()`
- In `processCommand`, call `undoManager.pushState(fieldId, originalText)` **after** a successful API result (inside the `result.isSuccess` block, before `replaceText`). This ensures failed or timed-out transforms do not create undo entries or clear the redo stack.
- Update the event-dispatch condition at line 130 (currently `command.trigger.endsWith("undo") && command.isBuiltIn`) to also match `?redo`: `(command.trigger.endsWith("undo") || command.trigger.endsWith("redo")) && command.isBuiltIn`
- Replace `handleUndo` with `handleUndoRedo` that handles both `?undo` and `?redo`:
  - `?undo`: call `undoManager.undo(fieldId)`, push current text to redo, replace field text
  - `?redo`: call `undoManager.redo(fieldId)`, push current text to undo, replace field text
- On empty stack (undo/redo returns null): replace field text with `cleanText` (trigger already stripped) to remove the typed trigger from the input, then show toast "Nothing to undo" / "Nothing to redo" with haptic reject. This preserves current UX where the trigger text is always cleaned from the field.

## Changes to `CommandManager.kt`

Add to `builtInDefinitions`:
```kotlin
"redo" to "Redo the last undone replacement."
```

## Error Handling

| Scenario | Behavior |
|---|---|
| Undo on empty stack | Toast "Nothing to undo", haptic reject |
| Redo on empty stack | Toast "Nothing to redo", haptic reject |
| Stale node reference | `source.refresh()` fails, toast "Could not restore text" (existing fallback) |
| Field evicted from LRU | `undo()`/`redo()` returns null, treated as empty stack |

## Testing

### New: `UndoManagerTest.kt`

File: `app/src/test/java/com/musheer360/swiftslate/manager/UndoManagerTest.kt`

Test cases:
- Push state and undo returns previous text
- Multiple pushes and sequential undos walk back through history
- Undo then redo restores the undone text
- Full undo/redo cycle preserves all states
- Redo stack cleared on new transformation
- Stack cap enforced at 5 — oldest entry dropped
- LRU eviction at 10 fields — oldest field's history is dropped
- Undo on empty stack returns null
- Redo on empty stack returns null
- Field ID derivation with and without `viewIdResourceName`

### Updated: `CommandManagerTest.kt`

- Update `getCommands_returnsNineBuiltInByDefault` to assert 10 built-in commands (was 9)
- Add test: `findCommand_withRedoTrigger_returnsRedoCommand` — verify `?redo` is found and marked as built-in
