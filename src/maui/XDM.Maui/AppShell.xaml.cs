using XDM.Maui.Views;

namespace XDM.Maui;

public partial class AppShell : Shell
{
    public AppShell()
    {
        InitializeComponent();
        Routing.RegisterRoute(nameof(DownloadPage), typeof(DownloadPage));
    }
}
