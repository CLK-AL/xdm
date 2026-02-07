# XDM Technology Comparison

A side-by-side tech comparison using XDM (Xtreme Download Manager) as a real-world
cross-platform desktop application to evaluate different frameworks and native compilation.

## Repository Structure

```
src/
├── java/     → Original Java source (Swing UI, ~194 files, JDK 11)
├── kmp/      → Kotlin Multiplatform conversion (Compose UI, GraalVM native)
│   ├── core/       → Shared download engine (commonMain)
│   ├── ui/         → Compose Multiplatform UI
│   ├── compose-app/→ Desktop entry point + GraalVM native-image
│   └── app/        → CLI entry point
├── maui/     → C# MAUI conversion (XAML UI, .NET Native AOT)
│   └── XDM.Maui/   → Cross-platform MAUI app
app/
├── XDM/
│   ├── XDM.Core/    → Existing C# download engine (reused by MAUI)
│   ├── XDM.Wpf.UI/  → Existing WPF UI (Windows)
│   └── XDM.Gtk.UI/  → Existing GTK3 UI (Linux)
```

---

## Part 1: UI Framework Comparison

### Original Implementations

| Aspect | Java Swing | WPF (C#) | GTK3 (C#) | Xamarin (deprecated) |
|--------|-----------|----------|----------|---------------------|
| **Language** | Java 11 | C# 9 / .NET 6 | C# 9 / .NET 6 | C# / Mono |
| **Rendering** | Java2D (AWT) | DirectX (WPF) | Cairo/Pango | Cocoa (AppKit) |
| **Layout** | BoxLayout, GridBag | Grid, DockPanel, Stack | Gtk.Box, Gtk.Grid | NSStackView |
| **Data binding** | Manual (listeners) | XAML {Binding} | Manual (signals) | XAML {Binding} |
| **Theming** | Custom LookAndFeel | ResourceDictionary | CSS (Adwaita) | Platform native |
| **Platforms** | Win/Lin/Mac (JVM) | Windows only | Linux only | macOS only |
| **Files** | 45 UI classes | 51 XAML + code-behind | ~30 GTK widgets | ~20 Xamarin views |
| **RAM** | ~40MB | ~20MB | ~15MB | ~25MB |
| **Code shared** | 0% across UIs | 0% with GTK/Xamarin | 0% with WPF/Xamarin | 0% with WPF/GTK |

**Problem**: 3 completely separate UI codebases for the same app. Xamarin deprecated.

### New Implementations

| Aspect | Compose Multiplatform (KMP) | .NET MAUI (C#) |
|--------|---------------------------|----------------|
| **Language** | Kotlin 2.1 | C# 13 / .NET 9 |
| **Rendering** | Skia (own canvas) | Platform native handlers |
| **Layout** | Row, Column, Box, LazyColumn | Grid, StackLayout, CollectionView |
| **Data binding** | Compose state + Flow | XAML {Binding} + MVVM |
| **Theming** | Material3 (dark/light) | AppThemeBinding (system) |
| **Platforms** | Win/Lin/Mac/iOS/Android/WASM | Win/Mac/iOS/Android (no Linux) |
| **Code shared** | ~95% (single UI codebase) | ~85% (platform handlers differ) |
| **Native build** | GraalVM CE native-image | .NET Native AOT |
| **RAM (native)** | ~15-25MB | ~15-25MB |
| **Binary size** | ~25-40MB | ~15-25MB |
| **Startup** | ~50ms (native) / ~2s (JVM) | ~50ms (AOT) |

---

## Part 2: Control-by-Control Mapping

How each WPF control maps to Compose and MAUI:

| WPF Control | Java Swing | Compose Multiplatform | MAUI |
|-------------|-----------|----------------------|------|
| `Window` | `JFrame` | `Window {}` | `ContentPage` |
| `Grid` | `GridBagLayout` | `Row/Column + weight()` | `Grid` |
| `DockPanel` | `BorderLayout` | `Column { top; Spacer(weight); bottom }` | `Grid` with rows |
| `StackPanel` | `BoxLayout` | `Row {}` / `Column {}` | `HorizontalStackLayout` / `VerticalStackLayout` |
| `ListBox` | `JList` | `LazyColumn + selectable` | `CollectionView` |
| `ListView + GridView` | `JTable` | `LazyColumn { items }` | `CollectionView + DataTemplate` |
| `Button` | `JButton` | `Button {}` / `TextButton {}` | `Button` |
| `TextBox` | `JTextField` | `OutlinedTextField {}` | `Entry` |
| `ProgressBar` | `JProgressBar` | `LinearProgressIndicator {}` | `ProgressBar` |
| `CheckBox` | `JCheckBox` | `Checkbox {}` | `Switch` or `CheckBox` |
| `ComboBox` | `JComboBox` | `ExposedDropdownMenuBox {}` | `Picker` |
| `TabControl` | `JTabbedPane` | `NavigationRail` / `TabRow` | `TabbedPage` |
| `ContextMenu` | `JPopupMenu` | `DropdownMenu {}` | `MenuFlyout` |
| `Path (vector)` | Custom paint | `Icon(imageVector)` | `FontImageSource` |
| `ScrollViewer` | `JScrollPane` | `verticalScroll(rememberScrollState())` | `ScrollView` |
| `Border` | Custom paint | `Card {}` / `Surface {}` | `Frame` / `Border` |
| `DataTemplate` | Custom renderer | `@Composable` function | `DataTemplate` |
| `{Binding}` | Listener pattern | `mutableStateOf()` / `StateFlow` | `{Binding}` with MVVM |
| `Trigger` | N/A | `if/when` in composition | `VisualStateManager` |
| `ResourceDictionary` | UIManager props | `MaterialTheme {}` | `ResourceDictionary` |

---

## Part 3: Download Engine Comparison

| Aspect | Java | C# (XDM.Core) | Kotlin (KMP) |
|--------|------|-------------|-------------|
| **Concurrency** | `Thread` + `synchronized` | `Thread` + `ReaderWriterLockSlim` | `coroutines` + `Mutex` |
| **HTTP client** | Custom socket-based | `HttpClient` + WinHTTP P/Invoke | Ktor (platform-native) |
| **Segments** | `Segment` interface + `SegmentImpl` | `Piece` + `PieceGrabber` | `Segment` data class |
| **Split/merge** | `findMaxChunk()` + `splitChunk()` | Similar logic in base class | Same algo, coroutine-safe |
| **State save** | Text file (`state.txt`) | Custom binary (`BinaryWriter`) | JSON (kotlinx.serialization) |
| **Config** | Text file (`config.txt`) | Custom binary (`settings.dat`) | JSON (`config.json`) |
| **Video extract** | `YoutubeDLHandler` (string concat) | `YDLProcess` (string concat) | `YtDlpWrapper` (arg list) |
| **FFmpeg** | `FFmpeg.java` (ProcessBuilder) | `FFmpegMediaProcessor` | `FFmpegRunner` (ProcessRunner) |
| **Browser IPC** | `ServerSocket` on :9614 | `NanoServer` HTTP + Named Pipe | Ktor server on :9614 |
| **TLS** | Java default (validates) | **Disabled** (`=> true`) | Ktor default (validates) |
| **Password storage** | Plaintext config | Plaintext binary file | JSON (should encrypt) |
| **Speed limiter** | `Thread.sleep()` per segment | `ManualResetEvent.WaitOne()` | `delay()` coroutine |
| **Assembly** | `FileInputStream` chain | `FileStream` chain | `FileInputStream` (JVM) |

---

## Part 4: Native Compilation Comparison

### GraalVM CE Native Image (KMP)

```bash
# Build native binary from Kotlin/JVM code
cd src/kmp/compose-app
./gradlew nativeCompile

# Or manually:
native-image --no-fallback -O2 \
  --initialize-at-build-time=kotlinx.serialization,kotlinx.coroutines \
  -jar build/libs/compose-app.jar \
  -o xdm-native
```

| Property | Value |
|----------|-------|
| **Compiler** | GraalVM CE 21 (free, open source) |
| **Input** | Kotlin bytecode (JVM .class files) |
| **Output** | Platform-native ELF/PE/Mach-O binary |
| **Binary size** | ~25-40MB (depends on included libs) |
| **RAM at runtime** | ~15-25MB (no JVM overhead) |
| **Startup time** | ~50ms (vs ~2s JVM cold start) |
| **GC** | Serial GC or G1 (substrate VM) |
| **Reflection** | Requires config files or build-time registration |
| **Static linking** | Supported (`--static --libc=musl` on Linux) |
| **Cross-compile** | No (must compile on target OS) |
| **Debug symbols** | Supported (`-g`) |
| **Limitations** | Dynamic class loading not supported; some reflection limits |

### .NET Native AOT (MAUI)

```bash
# Build native binary from C# code
cd src/maui/XDM.Maui
dotnet publish -c Release -r linux-x64 -p:PublishAot=true

# Output: bin/Release/net9.0/linux-x64/publish/XDM.Maui
```

| Property | Value |
|----------|-------|
| **Compiler** | RyuJIT AOT (.NET 9, built-in) |
| **Input** | C# IL (intermediate language) |
| **Output** | Platform-native binary |
| **Binary size** | ~15-25MB (aggressive trimming) |
| **RAM at runtime** | ~15-25MB |
| **Startup time** | ~50ms |
| **GC** | Workstation GC or Server GC |
| **Reflection** | Source generators preferred; rd.xml for manual config |
| **Static linking** | Supported on Linux |
| **Cross-compile** | Limited (publish from target OS recommended) |
| **Debug symbols** | Supported |
| **Limitations** | Some reflection-heavy patterns need adaptation |

### Head-to-Head

| Criterion | GraalVM CE (KMP) | .NET AOT (MAUI) |
|-----------|:---:|:---:|
| **Binary size** | 25-40MB | 15-25MB |
| **RAM** | 15-25MB | 15-25MB |
| **Startup** | ~50ms | ~50ms |
| **Build time** | 3-5 min | 1-3 min |
| **Linux support** | Native | Native (but MAUI UI needs Photino) |
| **Windows support** | Native | Native |
| **macOS support** | Native | Native |
| **Compilation cost** | Free (GraalVM CE) | Free (.NET SDK) |
| **Ecosystem maturity** | Growing (2-3 years) | Mature (5+ years AOT) |
| **Reflection handling** | Config JSON files | Source generators |
| **Serialization** | kotlinx.serialization (compile-time) | System.Text.Json (source gen) |
| **UI in native** | Compose (Skia canvas) | MAUI (native handlers) |

---

## Part 5: Code Reuse Summary

### What Each Version Reuses

| Layer | Java (original) | C# WPF+GTK | KMP (new) | MAUI (new) |
|-------|:-:|:-:|:-:|:-:|
| Download engine | Java only | C# only | **Kotlin (from Java)** | **C# (existing XDM.Core)** |
| HTTP networking | Java sockets | HttpClient + WinHTTP | **Ktor (cross-platform)** | **HttpClient (cross-platform)** |
| Browser monitor | Java sockets | NanoServer + pipes | **Ktor server** | **HttpListener/Kestrel** |
| Config/state | Java text | C# binary | **JSON (portable)** | **JSON (portable)** |
| Video/FFmpeg | Java process | C# process | **Kotlin process** | **C# process (existing)** |
| UI (Windows) | Swing | WPF | **Compose** | **MAUI (WinUI3)** |
| UI (Linux) | Swing | GTK3 | **Compose (same code!)** | No native (needs Photino) |
| UI (macOS) | Swing | Xamarin (dead) | **Compose (same code!)** | **MAUI (Mac Catalyst)** |
| UI code bases | **1** (Swing) | **3** (WPF+GTK+Xamarin) | **1** (Compose) | **1** (MAUI XAML) |

### Lines of Code (approximate)

| Component | Java | C# | KMP | MAUI |
|-----------|-----:|----:|----:|-----:|
| Download engine | 3,200 | 4,500 | 1,800 | reuses C# |
| HTTP/Network | 2,100 | 2,800 | 600 | reuses C# |
| Browser monitor | 400 | 900 | 200 | reuses C# |
| Config | 500 | 1,200 | 300 | reuses C# |
| Video/Media | 800 | 600 | 400 | reuses C# |
| UI | 4,800 | 8,200 (3 UIs) | 1,500 | 1,200 |
| **Total** | **11,800** | **18,200** | **4,800** | **1,200 + C# core** |

---

## Part 6: Verdict

| If you need... | Choose | Why |
|----------------|--------|-----|
| Max code reuse + Linux | **KMP + Compose** | One codebase for all platforms including Linux |
| Reuse existing C# code | **MAUI** | XDM.Core works as-is, just add MAUI UI |
| Lowest RAM | **Both are similar** | Native compilation levels the playing field |
| Best Windows experience | **MAUI** | Native WinUI3 widgets |
| Best Linux experience | **KMP + Compose** | MAUI has no Linux support |
| Web version too | **KMP** | Compose WASM or Kotlin/WASM |
| Smallest team | **MAUI** | Less code to write (reuses XDM.Core) |
| Learning exercise | **Both** | This repo demonstrates the tradeoffs side-by-side |

Both approaches converge to similar native performance via GraalVM CE / .NET AOT.
The choice is primarily about: **which language ecosystem** (Kotlin vs C#) and
**whether Linux is a must-have** (KMP wins) or **reusing existing C# code is priority** (MAUI wins).
