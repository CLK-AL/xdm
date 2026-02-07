using XDM.Maui.ViewModels;

namespace XDM.Maui.Views;

public partial class DownloadPage : ContentPage
{
    public DownloadPage(DownloadViewModel viewModel)
    {
        InitializeComponent();
        BindingContext = viewModel;
    }
}
