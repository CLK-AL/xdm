using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using XDM.Maui.Services;

namespace XDM.Maui.ViewModels;

/// <summary>
/// Main page view model showing download list.
/// Uses CommunityToolkit.Mvvm for source-generated INotifyPropertyChanged.
///
/// Maps to:
/// - Java MainWindow.java (Swing JFrame with JTable)
/// - C# WPF MainWindow.xaml.cs / GTK MainWindow.cs
/// </summary>
public partial class MainViewModel : ObservableObject
{
    private readonly IDownloadService _downloadService;

    [ObservableProperty]
    private ObservableCollection<DownloadItemViewModel> _downloads = new();

    [ObservableProperty]
    private DownloadItemViewModel? _selectedDownload;

    [ObservableProperty]
    private bool _isRefreshing;

    [ObservableProperty]
    private string _filterCategory = "All";

    public MainViewModel(IDownloadService downloadService)
    {
        _downloadService = downloadService;
        LoadDownloads();
    }

    [RelayCommand]
    private async Task AddDownload()
    {
        var url = await Shell.Current.DisplayPromptAsync("New Download", "Enter URL:");
        if (!string.IsNullOrWhiteSpace(url))
        {
            var id = await _downloadService.StartDownloadAsync(url);
            LoadDownloads();
        }
    }

    [RelayCommand]
    private async Task StopDownload(string id)
    {
        await _downloadService.StopDownloadAsync(id);
        LoadDownloads();
    }

    [RelayCommand]
    private async Task ResumeDownload(string id)
    {
        await _downloadService.ResumeDownloadAsync(id);
        LoadDownloads();
    }

    [RelayCommand]
    private async Task DeleteDownload(string id)
    {
        var confirm = await Shell.Current.DisplayAlert("Delete", "Remove this download?", "Yes", "No");
        if (confirm)
        {
            await _downloadService.CancelDownloadAsync(id);
            LoadDownloads();
        }
    }

    [RelayCommand]
    private async Task StopAll()
    {
        await _downloadService.StopAllAsync();
        LoadDownloads();
    }

    [RelayCommand]
    private void Refresh()
    {
        LoadDownloads();
        IsRefreshing = false;
    }

    private void LoadDownloads()
    {
        var items = _downloadService.GetAllDownloads();
        Downloads.Clear();
        foreach (var item in items)
        {
            Downloads.Add(new DownloadItemViewModel(item));
        }
    }
}

public partial class DownloadItemViewModel : ObservableObject
{
    [ObservableProperty] private string _id;
    [ObservableProperty] private string _fileName;
    [ObservableProperty] private string _folder;
    [ObservableProperty] private long _size;
    [ObservableProperty] private long _downloaded;
    [ObservableProperty] private int _progress;
    [ObservableProperty] private DownloadStatus _status;
    [ObservableProperty] private string _statusText;
    [ObservableProperty] private string _sizeText;

    public DownloadItemViewModel(DownloadItemInfo info)
    {
        _id = info.Id;
        _fileName = info.FileName;
        _folder = info.Folder;
        _size = info.Size;
        _downloaded = info.Downloaded;
        _progress = info.Progress;
        _status = info.Status;
        _statusText = info.Status.ToString();
        _sizeText = FormatSize(info.Size);
    }

    private static string FormatSize(long bytes) => bytes switch
    {
        < 0 => "Unknown",
        < 1024 => $"{bytes} B",
        < 1024 * 1024 => $"{bytes / 1024.0:F1} KB",
        < 1024 * 1024 * 1024 => $"{bytes / (1024.0 * 1024):F1} MB",
        _ => $"{bytes / (1024.0 * 1024 * 1024):F2} GB"
    };
}
