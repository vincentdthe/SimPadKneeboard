using Microsoft.Extensions.Logging;
using SimPad.Kneeboard.Server.Models;
using SimPad.Kneeboard.Server.Telemetry;

namespace SimPad.Kneeboard.Server.Services;

public interface IKneeboardHierarchyService
{
    List<KneeboardTab> ResolveTabs(string? profileId = null);
    KneeboardTab? GetTabById(string tabId, string? profileId = null);
    event EventHandler? TabsInvalidated;
}

public class KneeboardHierarchyService : IKneeboardHierarchyService
{
    private readonly ILogger<KneeboardHierarchyService> _logger;
    private readonly IProfileService _profileService;
    private readonly IDocumentService _documentService;
    private readonly ActiveSimManager _activeSimManager;

    public event EventHandler? TabsInvalidated;

    public KneeboardHierarchyService(
        ILogger<KneeboardHierarchyService> logger,
        IProfileService profileService,
        IDocumentService documentService,
        ActiveSimManager activeSimManager)
    {
        _logger = logger;
        _profileService = profileService;
        _documentService = documentService;
        _activeSimManager = activeSimManager;

        _activeSimManager.SimStateChanged += (sender, args) =>
        {
            _logger.LogInformation("Sim state changed in hierarchy service: {Sim}, Aircraft: {Aircraft}. Invalidating tabs.", args.SimulatorName, args.AircraftName);
            TabsInvalidated?.Invoke(this, EventArgs.Empty);
        };
    }

    public List<KneeboardTab> ResolveTabs(string? profileId = null)
    {
        var tabs = new List<KneeboardTab>();
        int order = 0;

        var profile = !string.IsNullOrEmpty(profileId)
            ? _profileService.GetProfileById(profileId)
            : _profileService.GetActiveProfile();

        profile ??= _profileService.GetActiveProfile();

        var activeSim = _activeSimManager.ActiveSimulator;
        var activeAircraft = _activeSimManager.ActiveAircraft;

        // 1. Dynamic System Tabs
        tabs.Add(new KneeboardTab
        {
            Id = "tab-dynamic-telemetry",
            Name = "Live Telemetry & Radios",
            Category = TabCategory.Dynamic,
            IsDynamic = true,
            DynamicType = "Radio",
            Order = order++,
            Pages = new List<KneeboardPage>
            {
                new()
                {
                    Id = "page-dynamic-telemetry",
                    TabId = "tab-dynamic-telemetry",
                    PageIndex = 0,
                    Title = "Live Telemetry & Radios",
                    FileType = PageFileType.Dynamic,
                    ContentUrl = "/api/telemetry"
                }
            }
        });

        tabs.Add(new KneeboardTab
        {
            Id = "tab-dynamic-notes",
            Name = "Quick Notes",
            Category = TabCategory.Dynamic,
            IsDynamic = true,
            DynamicType = "QuickNotes",
            Order = order++,
            Pages = new List<KneeboardPage>
            {
                new()
                {
                    Id = "page-dynamic-notes-1",
                    TabId = "tab-dynamic-notes",
                    PageIndex = 0,
                    Title = "Scratchpad Page 1",
                    FileType = PageFileType.Dynamic,
                    ContentUrl = "/api/kneeboard/notes/1"
                },
                new()
                {
                    Id = "page-dynamic-notes-2",
                    TabId = "tab-dynamic-notes",
                    PageIndex = 1,
                    Title = "Scratchpad Page 2",
                    FileType = PageFileType.Dynamic,
                    ContentUrl = "/api/kneeboard/notes/2"
                }
            }
        });

        // 2. Global Folders (Level 1)
        var userDocs = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
        var globalRoot = Path.Combine(userDocs, "Saved Games", "SimPadKneeboard", "Global");
        ScanDirectoryIntoTabs(tabs, globalRoot, "Global", TabCategory.Global, ref order);

        // 3. Simulator-Specific Folders (Level 2)
        if (activeSim == "DCS" || profile.Simulator == "DCS")
        {
            var dcsKneeboard = Path.Combine(userDocs, "Saved Games", "DCS", "KNEEBOARD");
            var dcsOpenBetaKneeboard = Path.Combine(userDocs, "Saved Games", "DCS.openbeta", "KNEEBOARD");

            ScanDirectFilesIntoTab(tabs, dcsKneeboard, "DCS General", TabCategory.Simulator, ref order);
            ScanDirectFilesIntoTab(tabs, dcsOpenBetaKneeboard, "DCS OpenBeta General", TabCategory.Simulator, ref order);
        }
        else if (activeSim == "FalconBMS" || profile.Simulator == "FalconBMS")
        {
            var bmsKneeboard = Path.Combine(userDocs, "Saved Games", "Falcon BMS", "Kneeboard");
            ScanDirectFilesIntoTab(tabs, bmsKneeboard, "Falcon BMS General", TabCategory.Simulator, ref order);
        }

        // 4. Module / Aircraft Specific Folders (Level 3)
        var effectiveAircraft = !string.IsNullOrEmpty(activeAircraft) && activeAircraft != "None" && activeAircraft != "Unknown"
            ? activeAircraft
            : profile.AircraftModule;

        if (!string.IsNullOrEmpty(effectiveAircraft))
        {
            var moduleSavedGames = Path.Combine(userDocs, "Saved Games", "DCS", "KNEEBOARD", effectiveAircraft);
            var moduleSavedGamesOB = Path.Combine(userDocs, "Saved Games", "DCS.openbeta", "KNEEBOARD", effectiveAircraft);

            ScanDirectoryIntoTabs(tabs, moduleSavedGames, $"{effectiveAircraft}", TabCategory.Module, ref order);
            ScanDirectoryIntoTabs(tabs, moduleSavedGamesOB, $"{effectiveAircraft} (OB)", TabCategory.Module, ref order);
        }

        // 5. Custom Profile Folders & WebViews (Level 4)
        if (profile.FolderSources != null)
        {
            foreach (var src in profile.FolderSources.Where(s => s.Enabled).OrderBy(s => s.Priority))
            {
                ScanDirectoryIntoTabs(tabs, src.Path, src.TabName, TabCategory.Custom, ref order, src.Recursive);
            }
        }

        if (profile.WebViewTabs != null)
        {
            foreach (var web in profile.WebViewTabs.Where(w => w.Enabled).OrderBy(w => w.Priority))
            {
                tabs.Add(new KneeboardTab
                {
                    Id = $"tab-webview-{web.Id}",
                    Name = web.TabName,
                    Category = TabCategory.WebView,
                    IsDynamic = true,
                    DynamicType = "WebView",
                    WebViewUrl = web.Url,
                    Order = order++,
                    Pages = new List<KneeboardPage>
                    {
                        new()
                        {
                            Id = $"page-webview-{web.Id}",
                            TabId = $"tab-webview-{web.Id}",
                            PageIndex = 0,
                            Title = web.TabName,
                            FileType = PageFileType.Dynamic,
                            ContentUrl = web.Url
                        }
                    }
                });
            }
        }

        return tabs;
    }

    public KneeboardTab? GetTabById(string tabId, string? profileId = null)
    {
        var all = ResolveTabs(profileId);
        return all.FirstOrDefault(t => t.Id == tabId);
    }

    private void ScanDirectoryIntoTabs(
        List<KneeboardTab> tabs,
        string rootPath,
        string baseName,
        TabCategory category,
        ref int order,
        bool recursive = true)
    {
        if (!Directory.Exists(rootPath))
            return;

        // Root files as primary tab
        var rootPages = _documentService.ScanFolderForPages($"tab-{Guid.NewGuid():N}", rootPath);
        if (rootPages.Count > 0)
        {
            var tabId = $"tab-{Guid.NewGuid():N}";
            foreach (var p in rootPages) p.TabId = tabId;
            
            tabs.Add(new KneeboardTab
            {
                Id = tabId,
                Name = baseName,
                Category = category,
                SourcePath = rootPath,
                Order = order++,
                Pages = rootPages
            });
        }

        // Subfolders as individual tabs
        if (recursive)
        {
            try
            {
                foreach (var subDir in Directory.GetDirectories(rootPath))
                {
                    var dirName = Path.GetFileName(subDir);
                    var subPages = _documentService.ScanFolderForPages($"tab-{Guid.NewGuid():N}", subDir);
                    if (subPages.Count > 0)
                    {
                        var tabId = $"tab-{Guid.NewGuid():N}";
                        foreach (var p in subPages) p.TabId = tabId;

                        tabs.Add(new KneeboardTab
                        {
                            Id = tabId,
                            Name = $"{baseName} / {dirName}",
                            Category = category,
                            SourcePath = subDir,
                            Order = order++,
                            Pages = subPages
                        });
                    }
                }
            }
            catch (Exception ex)
            {
                _logger.LogWarning(ex, "Error scanning subdirectories in {Path}", rootPath);
            }
        }
    }

    private void ScanDirectFilesIntoTab(
        List<KneeboardTab> tabs,
        string folderPath,
        string tabName,
        TabCategory category,
        ref int order)
    {
        if (!Directory.Exists(folderPath))
            return;

        var pages = _documentService.ScanFolderForPages($"tab-{Guid.NewGuid():N}", folderPath);
        if (pages.Count > 0)
        {
            var tabId = $"tab-{Guid.NewGuid():N}";
            foreach (var p in pages) p.TabId = tabId;

            tabs.Add(new KneeboardTab
            {
                Id = tabId,
                Name = tabName,
                Category = category,
                SourcePath = folderPath,
                Order = order++,
                Pages = pages
            });
        }
    }
}
