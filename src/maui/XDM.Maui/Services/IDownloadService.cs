namespace XDM.Maui.Services;

/// <summary>
/// Download service interface wrapping XDM.Core's ApplicationCore.
///
/// This maps directly to the existing C# download engine:
/// - StartDownload -> ApplicationCore.StartDownload()
/// - StopDownload  -> ApplicationCore.StopDownloads()
/// - ResumeDownload -> ApplicationCore.ResumeDownload()
///
/// The C# architecture is preserved exactly:
/// - HTTPDownloaderBase for single-source HTTP downloads
/// - MultiSourceDownloaderBase for DASH/HLS/HDS
/// - Piece/PieceGrabber for segment management
/// - SpeedLimiter for bandwidth control
/// </summary>
public interface IDownloadService
{
    Task<string> StartDownloadAsync(string url, string? fileName = null, string? folder = null);
    Task StopDownloadAsync(string id);
    Task ResumeDownloadAsync(string id);
    Task CancelDownloadAsync(string id);
    Task StopAllAsync();
    IObservable<DownloadProgressInfo> ProgressUpdates { get; }
    IObservable<DownloadCompletedInfo> CompletedUpdates { get; }
    List<DownloadItemInfo> GetAllDownloads();
    bool IsActive(string id);
}

public record DownloadProgressInfo(
    string Id,
    int Progress,
    long DownloadedBytes,
    long TotalBytes,
    long SpeedBytesPerSecond,
    string Eta
);

public record DownloadCompletedInfo(
    string Id,
    string FilePath,
    bool Success,
    string? ErrorMessage = null
);

public record DownloadItemInfo(
    string Id,
    string FileName,
    string Folder,
    long Size,
    long Downloaded,
    int Progress,
    DownloadStatus Status,
    DateTime DateAdded
);

public enum DownloadStatus
{
    Queued,
    Downloading,
    Stopped,
    Finished,
    Failed,
    Assembling,
    Converting
}
