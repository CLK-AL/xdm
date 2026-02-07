using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;

namespace XDM.Maui.ViewModels;

/// <summary>
/// Settings view model.
/// Maps to Java SettingsPage.java / C# SettingsWindow.
/// Wraps XDM.Core Config properties for MAUI binding.
/// </summary>
public partial class SettingsViewModel : ObservableObject
{
    [ObservableProperty] private int _maxSegments = 8;
    [ObservableProperty] private int _maxConcurrentDownloads = 5;
    [ObservableProperty] private int _speedLimitKbps;
    [ObservableProperty] private string _downloadFolder = "";
    [ObservableProperty] private string _tempFolder = "";
    [ObservableProperty] private bool _monitorClipboard;
    [ObservableProperty] private bool _monitorBrowser = true;
    [ObservableProperty] private bool _showNotification = true;
    [ObservableProperty] private bool _showDownloadCompleteWindow = true;
    [ObservableProperty] private string _proxyHost = "";
    [ObservableProperty] private int _proxyPort;
    [ObservableProperty] private int _selectedProxyType; // 0=None, 1=HTTP, 2=SOCKS5, 3=System

    public SettingsViewModel()
    {
        // In full implementation: load from Config.Instance
        DownloadFolder = Path.Combine(
            Environment.GetFolderPath(Environment.SpecialFolder.UserProfile), "Downloads");
        TempFolder = Path.GetTempPath();
    }

    [RelayCommand]
    private async Task BrowseDownloadFolder()
    {
        // MAUI folder picker
        var result = await FolderPicker.Default.PickAsync();
        if (result.IsSuccessful)
        {
            DownloadFolder = result.Folder.Path;
        }
    }

    [RelayCommand]
    private void Save()
    {
        // In full implementation:
        // Config.Instance.MaxSegments = MaxSegments;
        // Config.Instance.MaxConcurrentDownloads = MaxConcurrentDownloads;
        // etc.
        // Config.Save();
    }
}
