namespace SimPad.Kneeboard.Server.Models;

public enum TabCategory
{
    Global,
    Simulator,
    Module,
    Custom,
    Dynamic,
    WebView
}

public class KneeboardTab
{
    public string Id { get; set; } = Guid.NewGuid().ToString();
    public string Name { get; set; } = "";
    public TabCategory Category { get; set; }
    public string? SourcePath { get; set; }
    public string? WebViewUrl { get; set; }
    public int Order { get; set; }
    public int PageCount => Pages.Count;
    public List<KneeboardPage> Pages { get; set; } = new();
    public bool IsDynamic { get; set; }
    public string? DynamicType { get; set; } // "Radio", "FlightData", "QuickNotes", "WebView"
}

public enum PageFileType
{
    PDF,
    PNG,
    JPG,
    Dynamic
}

public class KneeboardPage
{
    public string Id { get; set; } = Guid.NewGuid().ToString();
    public string TabId { get; set; } = "";
    public int PageIndex { get; set; }
    public string Title { get; set; } = "";
    public PageFileType FileType { get; set; }
    public string? FilePath { get; set; }
    public int PdfPageNumber { get; set; } = 1;
    public int TotalPdfPages { get; set; } = 1;
    public long FileSizeBytes { get; set; }
    public DateTime LastModifiedUtc { get; set; }
    public string? ContentUrl { get; set; }
}
