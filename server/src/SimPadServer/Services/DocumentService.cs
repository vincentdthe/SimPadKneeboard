using System.Text;
using System.Text.RegularExpressions;
using Microsoft.Extensions.Logging;
using SimPad.Kneeboard.Server.Models;

namespace SimPad.Kneeboard.Server.Services;

public interface IDocumentService
{
    int GetPdfPageCount(string filePath);
    PageFileType GetFileType(string filePath);
    string GetContentType(string filePath);
    Stream? OpenRead(string filePath);
    List<KneeboardPage> ScanFolderForPages(string tabId, string folderPath);
}

public class DocumentService : IDocumentService
{
    private readonly ILogger<DocumentService> _logger;
    private readonly Dictionary<string, (int PageCount, DateTime LastModified)> _pdfCache = new();
    private readonly object _lock = new();

    public DocumentService(ILogger<DocumentService> logger)
    {
        _logger = logger;
    }

    public PageFileType GetFileType(string filePath)
    {
        var ext = Path.GetExtension(filePath).ToLowerInvariant();
        return ext switch
        {
            ".pdf" => PageFileType.PDF,
            ".png" => PageFileType.PNG,
            ".jpg" or ".jpeg" => PageFileType.JPG,
            _ => PageFileType.PNG
        };
    }

    public string GetContentType(string filePath)
    {
        var ext = Path.GetExtension(filePath).ToLowerInvariant();
        return ext switch
        {
            ".pdf" => "application/pdf",
            ".png" => "image/png",
            ".jpg" or ".jpeg" => "image/jpeg",
            _ => "application/octet-stream"
        };
    }

    public Stream? OpenRead(string filePath)
    {
        if (!File.Exists(filePath))
            return null;

        return new FileStream(filePath, FileMode.Open, FileAccess.Read, FileShare.ReadWrite);
    }

    public int GetPdfPageCount(string filePath)
    {
        if (!File.Exists(filePath))
            return 0;

        try
        {
            var lastWrite = File.GetLastWriteTimeUtc(filePath);
            lock (_lock)
            {
                if (_pdfCache.TryGetValue(filePath, out var cached) && cached.LastModified == lastWrite)
                {
                    return cached.PageCount;
                }
            }

            int count = ParsePdfPageCount(filePath);
            lock (_lock)
            {
                _pdfCache[filePath] = (count, lastWrite);
            }
            return count;
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "Error reading PDF page count for {File}", filePath);
            return 1;
        }
    }

    private int ParsePdfPageCount(string filePath)
    {
        try
        {
            using var fs = new FileStream(filePath, FileMode.Open, FileAccess.Read, FileShare.ReadWrite);
            using var reader = new StreamReader(fs, Encoding.ASCII);
            var content = reader.ReadToEnd();

            var match = Regex.Match(content, @"/Type\s*/Pages.*?/Count\s+(\d+)", RegexOptions.Singleline | RegexOptions.IgnoreCase);
            if (match.Success && int.TryParse(match.Groups[1].Value, out int count))
            {
                return count;
            }

            var matches = Regex.Matches(content, @"/Type\s*/Page[^s]", RegexOptions.IgnoreCase);
            if (matches.Count > 0)
            {
                return matches.Count;
            }
        }
        catch
        {
            // fallback
        }
        return 1;
    }

    public List<KneeboardPage> ScanFolderForPages(string tabId, string folderPath)
    {
        var pages = new List<KneeboardPage>();
        if (!Directory.Exists(folderPath))
            return pages;

        try
        {
            var files = Directory.GetFiles(folderPath)
                .Where(f =>
                {
                    var ext = Path.GetExtension(f).ToLowerInvariant();
                    return ext is ".pdf" or ".png" or ".jpg" or ".jpeg";
                })
                .OrderBy(f => f, StringComparer.OrdinalIgnoreCase)
                .ToList();

            int pageIndex = 0;
            foreach (var file in files)
            {
                var fileInfo = new FileInfo(file);
                var fileType = GetFileType(file);
                
                if (fileType == PageFileType.PDF)
                {
                    int totalPdfPages = GetPdfPageCount(file);
                    for (int pdfPageNum = 1; pdfPageNum <= totalPdfPages; pdfPageNum++)
                    {
                        pages.Add(new KneeboardPage
                        {
                            TabId = tabId,
                            PageIndex = pageIndex++,
                            Title = totalPdfPages > 1 
                                ? $"{Path.GetFileNameWithoutExtension(file)} (Page {pdfPageNum}/{totalPdfPages})"
                                : Path.GetFileNameWithoutExtension(file),
                            FileType = PageFileType.PDF,
                            FilePath = file,
                            PdfPageNumber = pdfPageNum,
                            TotalPdfPages = totalPdfPages,
                            FileSizeBytes = fileInfo.Length,
                            LastModifiedUtc = fileInfo.LastWriteTimeUtc,
                            ContentUrl = $"/api/kneeboard/pages/content?path={Uri.EscapeDataString(file)}&page={pdfPageNum}"
                        });
                    }
                }
                else
                {
                    pages.Add(new KneeboardPage
                    {
                        TabId = tabId,
                        PageIndex = pageIndex++,
                        Title = Path.GetFileNameWithoutExtension(file),
                        FileType = fileType,
                        FilePath = file,
                        PdfPageNumber = 1,
                        TotalPdfPages = 1,
                        FileSizeBytes = fileInfo.Length,
                        LastModifiedUtc = fileInfo.LastWriteTimeUtc,
                        ContentUrl = $"/api/kneeboard/pages/content?path={Uri.EscapeDataString(file)}"
                    });
                }
            }
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to scan folder {Folder} for kneeboard pages", folderPath);
        }

        return pages;
    }
}
