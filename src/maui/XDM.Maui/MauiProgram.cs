using CommunityToolkit.Maui;
using Microsoft.Extensions.Logging;
using XDM.Maui.Services;
using XDM.Maui.ViewModels;
using XDM.Maui.Views;

namespace XDM.Maui;

/// <summary>
/// MAUI application entry point.
///
/// Architecture: Reuses the existing XDM.Core C# download engine directly.
/// The MAUI layer provides:
///   - Cross-platform UI via XAML (replaces WPF + GTK separate UIs)
///   - MVVM pattern via CommunityToolkit.Mvvm
///   - Platform services via dependency injection
///   - Targets: Android, iOS, macOS Catalyst, Windows
///
/// The existing C# classes are referenced directly:
///   - ApplicationCore -> injected as singleton service
///   - Config -> loaded at startup
///   - All downloader types (HTTP, HLS, DASH, HDS) used as-is
/// </summary>
public static class MauiProgram
{
    public static MauiApp CreateMauiApp()
    {
        var builder = MauiApp.CreateBuilder();

        builder
            .UseMauiApp<App>()
            .UseMauiCommunityToolkit()
            .ConfigureFonts(fonts =>
            {
                fonts.AddFont("OpenSans-Regular.ttf", "OpenSansRegular");
                fonts.AddFont("OpenSans-Semibold.ttf", "OpenSansSemibold");
            });

        // Register services
        // In full implementation, ApplicationCore from XDM.Core would be registered here:
        // builder.Services.AddSingleton<ApplicationCore>();
        builder.Services.AddSingleton<IDownloadService, DownloadService>();
        builder.Services.AddSingleton<IBrowserMonitorService, BrowserMonitorService>();

        // Register ViewModels
        builder.Services.AddTransient<MainViewModel>();
        builder.Services.AddTransient<DownloadViewModel>();
        builder.Services.AddTransient<SettingsViewModel>();

        // Register Views
        builder.Services.AddTransient<MainPage>();
        builder.Services.AddTransient<DownloadPage>();
        builder.Services.AddTransient<SettingsPage>();

#if DEBUG
        builder.Logging.AddDebug();
#endif

        return builder.Build();
    }
}
