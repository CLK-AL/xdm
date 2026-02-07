namespace XDM.Maui.Services;

/// <summary>
/// Browser monitor service interface.
/// Wraps C# BrowserMonitoring/IpcHttpMessageProcessor from XDM.Core.
/// </summary>
public interface IBrowserMonitorService
{
    void Start();
    void Stop();
    bool IsRunning { get; }
    IObservable<BrowserDownloadRequest> DownloadRequests { get; }
    IObservable<VideoFoundInfo> VideoFound { get; }
}

public record BrowserDownloadRequest(
    string Url,
    string FileName,
    Dictionary<string, string> Headers,
    Dictionary<string, string> Cookies
);

public record VideoFoundInfo(
    string Id,
    string Title,
    string Description
);

/// <summary>
/// Implementation wrapping the existing C# NanoServer + IpcHttpMessageProcessor.
/// On Android/iOS, this would use a different IPC mechanism.
/// </summary>
public class BrowserMonitorService : IBrowserMonitorService
{
    // In full implementation: private readonly IpcHttpMessageProcessor _processor;
    private bool _isRunning;

    public bool IsRunning => _isRunning;
    public IObservable<BrowserDownloadRequest> DownloadRequests => throw new NotImplementedException();
    public IObservable<VideoFoundInfo> VideoFound => throw new NotImplementedException();

    public void Start()
    {
        _isRunning = true;
        // _processor = new IpcHttpMessageProcessor(this);
        // _processor.Run();
    }

    public void Stop()
    {
        _isRunning = false;
        // _processor?.Stop();
    }
}
