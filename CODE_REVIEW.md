# XDM Code Review

Comprehensive security and code quality review of the Xtreme Download Manager codebase.

---

## Critical Findings

### 1. TLS/SSL Certificate Validation Completely Disabled

Certificate validation is unconditionally bypassed across **every** HTTP client implementation, making the application vulnerable to man-in-the-middle (MITM) attacks on all HTTPS connections.

| Location | Code |
|----------|------|
| `app/XDM/XDM.Core/Clients/Http/DotNetHttpClient.cs:56` | `ServerCertificateCustomValidationCallback = (a, b, c, d) => true` |
| `app/XDM/XDM.Core/Clients/Http/WinHttpClient.cs:119-134` | All `SECURITY_FLAG_IGNORE_*` flags set |
| `app/XDM/XDM.Wpf.UI/App.xaml.cs:31` | `ServicePointManager.ServerCertificateValidationCallback += (a, b, c, d) => true` |
| `app/XDM/XDM.Gtk.UI/Program.cs:66` | Same global bypass |

**Impact:** The application downloads updates from GitHub with no certificate validation. An attacker on the same network could serve a malicious update binary and achieve arbitrary code execution. Additionally, `DontEnableSchUseStrongCrypto` is actively set to `true` (App.xaml.cs:42), and TLS 1.0 is enabled (WinHttpClient.cs:92).

### 2. Command Injection in YDLProcess

**File:** `app/XDM/XDM.Core/YDLWrapper/YDLProcess.cs:43-60`

The `Uri`, `UserName`, `Password`, and `BrowserName` properties are interpolated directly into a command string via `StringBuilder` concatenation without escaping:

```csharp
sb.Append($"--cookies-from-browser {BrowserName}");
sb.Append(" --username ").Append(UserName);
sb.Append(" --password ").Append(Password);
```

A crafted URL, browser name, or credentials could inject arbitrary arguments into the yt-dlp process. Unlike `FFmpegMediaProcessor` which uses `ArgumentList.Add()`, this code uses raw string concatenation.

### 3. Zip Slip in Import

**File:** `app/XDM/XDM.Core/ImportExport.cs:17-41`

`ZipFile.ExtractToDirectory(tempDir)` extracts user-supplied zip archives without validating entry paths. Malicious archives containing `../../` paths could write files outside the temp directory.

---

## High Severity

### 4. Plaintext Password Storage

**File:** `app/XDM/XDM.Core/IO/ConfigIO.cs:387-396`

User credentials (host/username/password) are serialized to `settings.dat` in plaintext with no encryption. Proxy passwords are also stored in plaintext in `.state` files (`DownloadStateIO.cs:143-148, 258-263`). Passwords are also visible in process listings when passed as yt-dlp arguments.

### 5. Credential Leak via Hostname Substring Matching

**File:** `app/XDM/XDM.Core/Util/Helpers.cs:143-154`

```csharp
if (host.Contains(item.Host))
```

Stored credentials with `Host = "example"` will match `evil-example.com`, `example.evil.com`, or any domain containing that substring. Credentials can leak to unintended servers.

### 6. Named Pipe with No Access Control

**File:** `app/XDM/NativeMessagingHost/NativeMessagingHostApp.cs:174`

The pipe `XDM_Ipc_Browser_Monitoring_Pipe` uses a well-known hardcoded name with no authentication. Any local process can create a pipe server with this name before XDM starts, intercepting all browser messages (URLs, cookies, headers).

### 7. Event Handler Leak Bug (+=  instead of -=)

**File:** `app/XDM/XDM.Core/ApplicationCore.cs:621`

```csharp
download.AssembingProgressChanged += AssembleProgressChanged; // BUG: should be -=
```

In `DetachEventHandlers`, this line attaches the handler a second time instead of detaching it, causing memory leaks and duplicate event firing.

### 8. Proxy Credential Bug -- Wrong Source Used

**File:** `app/XDM/XDM.Core/Clients/Http/WinHttpClient.cs:145-152`

The condition checks `this.proxy.Value.UserName` but passes `authentication.Value.UserName` (server auth, not proxy auth) to `WinHttpSetCredentials`. Server credentials may leak to the proxy server, and proxy auth will fail.

---

## Medium Severity

### 9. HTTP Server DoS Vectors

**File:** `app/XDM/XDM.Core/HttpServer/HttpParser.cs:80-87`

- Unbounded memory allocation from `Content-Length` header -- no maximum size check
- No limit on header count or line length (`LineReader.cs`)
- Each connection spawns a new `Thread` with no pool or limit (`HttpServer.cs:40-64`)

### 10. Race Condition in CountdownLatch

**File:** `app/XDM/XDM.Core/Downloader/CountdownLatch.cs:16-20`

```csharp
Interlocked.Decrement(ref counter);
if (counter == 0) this.Latch.Set();  // non-atomic read after atomic decrement
```

Should be `if (Interlocked.Decrement(ref counter) == 0)` to use the return value atomically.

### 11. HTTP Header / Cookie Injection

- `WinHttpClient.cs:285-318` -- headers assembled via string concatenation with `\r\n` delimiters; CRLF in values enables injection
- `WinHttpClient.cs:297-307` -- cookie values appended without sanitization
- `DotNetHttpClient.cs:174-205` -- `TryAddWithoutValidation` bypasses header validation

### 12. Inconsistent Thread Safety on `liveDownloads`

**File:** `app/XDM/XDM.Core/ApplicationCore.cs:35`

The `liveDownloads` dictionary is only protected by `lock(this)` in some handlers (`DownloadFinished`, `DownloadFailed`, `DownloadCancelled`) but not in `StartDownload` (line 126), `ResumeDownload` (line 250), or `StopDownloads` (line 347).

### 13. `stopRequested` Not Volatile

**Files:** `HTTPDownloaderBase.cs:41`, `MultiSourceDownloaderBase.cs:67`

The `stopRequested` boolean is read/written from multiple threads without `volatile` or synchronization.

### 14. Command Injection in SpawnSubProcess

**File:** `app/XDM/XDM.Core/Util/PlatformHelper.cs:473-489`

Arguments joined with `string.Join(" ", args)` with no quoting. Affects `RunCommand`, `RunAntivirus`, and `ShutDownPC`.

### 15. Browser Extension -- Plaintext HTTP Fallback

**File:** `app/xdm-browser-monitor/chrome/messaging.js:3`

When native messaging is unavailable, the extension sends URLs, headers, and cookies over unauthenticated HTTP to `http://127.0.0.1:9614`. Any local process can impersonate this endpoint.

---

## Low Severity

### 16. TransactedIO.WriteStream Returns False on Success

**File:** `app/XDM/XDM.Core/IO/TransactedIO.cs:159-186`

After the first write, the method returns `false` on success (line 179). Callers cannot distinguish successful writes from failures.

### 17. MemoryStream Not Seeked in NET35 Path

**File:** `app/XDM/XDM.Core/Downloader/Progressive/HTTPDownloaderBase.cs:598-603`

After `stream.CopyTo(ms)`, `ms.Position` is at the end. Missing `ms.Position = 0` causes chunk state restoration to always fail on .NET 3.5.

### 18. Browser Extension Bugs

- **`addToValueList`** (`chrome/util.js:170-176`, `chrome/bg3.js:60-66`) -- always overwrites array due to missing `else`, losing duplicate header values
- **`sendImageToXDM`** (`chrome/bg2.js:180`) -- `url = info.srcUrl` missing `var`, creating global variable
- **`log` in firefox-old** (`firefox-old/bg.js:20-28`) -- calls itself recursively instead of `console.log`; stack overflow if debug enabled
- **`sendRecUrl`** (`chrome/messaging.js:93`) -- off-by-one skips last URL in array
- **`sendWithNativeMessaging`** (`chrome/messaging.js:192`) -- `data +=` on an object coerces to `"[object Object]"`, corrupting the message
- **Debug mode** hardcoded to `true` in production (`chrome/bg2.js:218`, `chrome/bg3.js:15`)

### 19. Resource Leaks

- `ManualResetEvent` never disposed in `HttpChunkDownloader`, `SpeedLimiter`, `CountdownLatch`
- `ReaderWriterLockSlim` never disposed in `HTTPDownloaderBase`, `MultiSourceDownloaderBase`
- `PROCESS_INFORMATION` handles leaked in `NativeProcess.cs:23-25`

### 20. Incomplete File Name Sanitization

**File:** `app/XDM/XDM.Core/Util/FileHelper.cs:15-20`

`SanitizeFileName` does not handle Windows reserved device names (CON, PRN, NUL, COM1, etc.) or null bytes.

---

## Summary

| Severity | Count | Categories |
|----------|-------|------------|
| **Critical** | 3 | TLS bypass, command injection, zip slip |
| **High** | 5 | Plaintext credentials, credential leak, named pipe hijack, event handler bug, proxy credential bug |
| **Medium** | 7 | HTTP server DoS, race conditions, header injection, thread safety, command injection, browser HTTP fallback |
| **Low** | 5 | Logic bugs, browser extension bugs, resource leaks, sanitization gaps |

The most architecturally significant issue is the **complete disabling of TLS certificate validation** across all HTTP clients, which undermines HTTPS security for every download and the update mechanism. The second most critical pattern is the **plaintext handling of credentials** throughout the application (storage, transmission, process arguments, and hostname-substring matching for credential selection).
