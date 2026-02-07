using System.Reactive.Linq;
using System.Reactive.Subjects;

namespace XDM.Maui.Services;

/// <summary>
/// Download service implementation.
/// In the full implementation, this wraps XDM.Core.ApplicationCore
/// and bridges its event-based API to Rx observables for MAUI binding.
///
/// The existing C# download engine is used directly:
/// - SingleSourceHTTPDownloader for regular HTTP downloads
/// - MultiSourceHLSDownloader for HLS streams
/// - MultiSourceDASHDownloader for DASH streams
/// - All existing Piece/PieceGrabber/SpeedLimiter code
/// </summary>
public class DownloadService : IDownloadService
{
    // In full implementation: private readonly ApplicationCore _core;
    private readonly Subject<DownloadProgressInfo> _progressSubject = new();
    private readonly Subject<DownloadCompletedInfo> _completedSubject = new();
    private readonly Dictionary<string, DownloadItemInfo> _downloads = new();
    private readonly object _lock = new();

    public IObservable<DownloadProgressInfo> ProgressUpdates => _progressSubject.AsObservable();
    public IObservable<DownloadCompletedInfo> CompletedUpdates => _completedSubject.AsObservable();

    public DownloadService()
    {
        // In full implementation:
        // _core = new ApplicationCore();
        // _core.DownloadProgressChanged += OnProgressChanged;
        // _core.DownloadFinished += OnFinished;
        // _core.DownloadFailed += OnFailed;
    }

    public Task<string> StartDownloadAsync(string url, string? fileName = null, string? folder = null)
    {
        var id = Guid.NewGuid().ToString("N")[..8];

        lock (_lock)
        {
            _downloads[id] = new DownloadItemInfo(
                Id: id,
                FileName: fileName ?? GetFileNameFromUrl(url),
                Folder: folder ?? GetDefaultDownloadFolder(),
                Size: -1,
                Downloaded: 0,
                Progress: 0,
                Status: DownloadStatus.Queued,
                DateAdded: DateTime.Now
            );
        }

        // In full implementation:
        // _core.StartDownload(metadata, fileName, folder, ...);

        return Task.FromResult(id);
    }

    public Task StopDownloadAsync(string id)
    {
        // _core.StopDownloads(new[] { id }, false);
        return Task.CompletedTask;
    }

    public Task ResumeDownloadAsync(string id)
    {
        // _core.ResumeDownload(id, false);
        return Task.CompletedTask;
    }

    public Task CancelDownloadAsync(string id)
    {
        // _core.StopDownloads(new[] { id }, true);
        lock (_lock) { _downloads.Remove(id); }
        return Task.CompletedTask;
    }

    public Task StopAllAsync()
    {
        // _core.StopDownloads(_downloads.Keys.ToArray(), false);
        return Task.CompletedTask;
    }

    public List<DownloadItemInfo> GetAllDownloads()
    {
        lock (_lock) { return _downloads.Values.ToList(); }
    }

    public bool IsActive(string id) => _downloads.TryGetValue(id, out var d) && d.Status == DownloadStatus.Downloading;

    private static string GetFileNameFromUrl(string url)
    {
        var uri = new Uri(url);
        var path = uri.AbsolutePath;
        var name = path.Split('/').LastOrDefault(s => !string.IsNullOrEmpty(s)) ?? "download";
        return name;
    }

    private static string GetDefaultDownloadFolder()
    {
        return Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.UserProfile), "Downloads");
    }
}
