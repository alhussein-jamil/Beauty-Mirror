Beauty Mirror 1.3.3

- Removed the explicit androidx.compose.foundation.layout.weight import from DebugOverlay.kt.
- The RowScope Modifier.weight(...) calls remain valid without that import.
- Fixes the Kotlin compiler error: Cannot access RowColumnParentData?.weight.
