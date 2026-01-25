# NFX Native Library

C++/JNI native code for the NFX library. This module provides the Windows DWM integration and custom window procedure handling.

## Requirements

- **Compiler**: Visual Studio 2019+ (MSVC)
- **IDE**: CLion or Visual Studio
- **SDK**: Windows 10 SDK (10.0.19041.0 or later)
- **JDK**: JDK 25+ with JNI headers

## Project Structure

```
src/native/
├── CMakeLists.txt          # CMake build configuration
├── src/
│   ├── jni_h/              # Generated JNI headers
│   ├── utils/
│   │   ├── HwndMap.h/.cpp  # Thread-safe HWND-to-object mapping
│   │   ├── NfxWinProc.h    # Window procedure handler
│   │   └── utils.h/.cpp    # Utility functions
│   ├── Nfx.cpp             # NfxUtil JNI implementations
│   └── NfxAbstract.cpp     # AbstractNfxUndecoratedWindow JNI implementations
└── README.md
```

## Building

### With CLion

1. Open the `nfx-core/src/native` folder in CLion
2. Configure the toolchain to use Visual Studio compiler
3. Set CMake options:
   ```
   -DJAVA_HOME=<path-to-jdk>
   ```
4. Build the `nfx-core` target

### With Visual Studio

1. Generate VS project files with CMake:
   ```cmd
   cmake -G "Visual Studio 17 2022" -A x64 -B build
   ```
2. Open `build/nfx-core.sln`
3. Build in Release configuration

### Output

The built DLL is placed at:
```
nfx-core/src/main/resources/lib/nfx-core-win64-{version}.dll
```

## Architecture

### Window Procedure Subclassing

The native code subclasses the JavaFX window's WndProc to intercept Windows messages:

```
┌────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│ Windows Shell  │────▶│ Custom WndProc   │────▶│ Original WndProc│
│ (messages)     │     │ (NfxWinProc)     │     │ (JavaFX Glass)  │
└────────────────┘     └────────┬─────────┘     └─────────────────┘
                                │
                       ┌────────▼─────────┐
                       │ JNI Callbacks    │
                       │ (Java methods)   │
                       └──────────────────┘
```

### Key Messages Handled

| Message | Purpose |
|---------|---------|
| `WM_NCCALCSIZE` | Remove standard window frame |
| `WM_NCHITTEST` | Custom hit-testing for title bar |
| `WM_NCACTIVATE` | Handle activation state |
| `WM_GETMINMAXINFO` | Control maximize bounds |
| `WM_DPICHANGED` | Per-monitor DPI handling |

### Thread Safety

The native code runs on the Windows message thread, not the JavaFX Application Thread. Key safety mechanisms:

- **JniAttachGuard**: Automatically attaches/detaches threads for JNI calls
- **HwndMap**: Thread-safe HWND-to-jobject mapping with critical sections
- **std::call_once**: Thread-safe static initialization
- **Exception clearing**: All JNI calls check for and clear exceptions

## JNI Methods

### NfxUtil (Nfx.cpp)

| Method | Description |
|--------|-------------|
| `getNativeHandle` | Extract HWND from JavaFX Stage |
| `setTitleBarColor` | Set DWMWA_CAPTION_COLOR |
| `setTextColor` | Set DWMWA_TEXT_COLOR |
| `setCornerPreference` | Set DWMWA_WINDOW_CORNER_PREFERENCE |
| `setBorderColor` | Set DWMWA_BORDER_COLOR |
| `focusWindow` | Find and focus window by title |

### AbstractNfxUndecoratedWindow (NfxAbstract.cpp)

| Method | Description |
|--------|-------------|
| `install` | Subclass window procedure |
| `uninstall` | Restore original window procedure |
| `update` | Update window frame for maximize/fullscreen |
| `hideFromTaskBar` | Toggle taskbar visibility |

### JNI Callbacks (Java → Native → Java)

| Callback | Purpose |
|----------|---------|
| `jniHitTest` | Perform custom hit-testing |
| `jniIsFullScreen` | Query fullscreen state |
| `jniIsMaximized` | Query maximized state |
| `jniFireStateChanged` | Notify state changes |
| `jniInvalidateSpots` | Invalidate hit spot cache |

## DWM Integration

The native code uses the Desktop Window Manager (DWM) API:

```cpp
// Example: Set window corner preference
DWM_WINDOW_CORNER_PREFERENCE pref = DWMWCP_ROUND;
DwmSetWindowAttribute(hwnd, DWMWA_WINDOW_CORNER_PREFERENCE, &pref, sizeof(pref));
```

Required headers:
- `dwmapi.h` - DWM API
- `windowsx.h` - Window macros
- `shellapi.h` - Shell integration

## Debugging

1. Attach Visual Studio debugger to the Java process
2. Set breakpoints in native code
3. Use OutputDebugString for logging:
   ```cpp
   OutputDebugStringW(L"Debug message\n");
   ```

## Common Issues

### JNI_OnLoad not called
Ensure the DLL is loaded via `System.load()` not `System.loadLibrary()`.

### Access violation in JNI callback
Check that the Java object reference is valid (global ref, not local).

### DWM attributes not applying
Verify Windows version supports the attribute (some require Windows 11).
