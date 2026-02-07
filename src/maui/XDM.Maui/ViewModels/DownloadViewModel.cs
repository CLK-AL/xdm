using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using XDM.Maui.Services;

namespace XDM.Maui.ViewModels;

/// <summary>
/// Individual download progress view model.
/// Maps to Java DownloadWindow.java / C# DownloadProgressWindow.
/// </summary>
public partial class DownloadViewModel : ObservableObject, IQueryAttributable
{
    private readonly IDownloadService _downloadService;
    private IDisposable? _progressSubscription;

    [ObservableProperty] private string _downloadId = "";
    [ObservableProperty] private string _fileName = "";
    [ObservableProperty] private int _progress;
    [ObservableProperty] private string _downloadedText = "0 B";
    [ObservableProperty] private string _totalText = "Unknown";
    [ObservableProperty] private string _speedText = "0 B/s";
    [ObservableProperty] private string _etaText = "--";
    [ObservableProperty] private string _statusText = "Starting...";

    public DownloadViewModel(IDownloadService downloadService)
    {
        _downloadService = downloadService;
    }

    public void ApplyQueryAttributes(IDictionary<string, object> query)
    {
        if (query.TryGetValue("id", out var id))
        {
            DownloadId = id.ToString()!;
            StartObserving();
        }
    }

    private void StartObserving()
    {
        _progressSubscription = _downloadService.ProgressUpdates
            .Subscribe(info =>
            {
                if (info.Id != DownloadId) return;

                MainThread.BeginInvokeOnMainThread(() =>
                {
                    Progress = info.Progress;
                    SpeedText = FormatSpeed(info.SpeedBytesPerSecond);
                    EtaText = info.Eta;
                    StatusText = $"Downloading... {info.Progress}%";
                });
            });
    }

    [RelayCommand]
    private async Task Stop()
    {
        await _downloadService.StopDownloadAsync(DownloadId);
        StatusText = "Stopped";
        await Shell.Current.GoToAsync("..");
    }

    private static string FormatSpeed(long bps) => bps switch
    {
        <= 0 => "0 B/s",
        < 1024 => $"{bps} B/s",
        < 1024 * 1024 => $"{bps / 1024.0:F1} KB/s",
        _ => $"{bps / (1024.0 * 1024):F1} MB/s"
    };
}
