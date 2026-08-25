using System.Text.Json;
using Microsoft.Extensions.Logging;
using SimPad.Kneeboard.Server.Models;

namespace SimPad.Kneeboard.Server.Services;

public interface IProfileService
{
    List<Profile> GetAllProfiles();
    Profile? GetProfileById(string id);
    Profile GetActiveProfile();
    Profile CreateOrUpdateProfile(Profile profile);
    bool DeleteProfile(string id);
    bool SetActiveProfile(string id);
}

public class ProfileService : IProfileService
{
    private readonly ILogger<ProfileService> _logger;
    private readonly string _storageFilePath;
    private readonly List<Profile> _profiles = new();
    private readonly object _lock = new();
    private string _activeProfileId = "";

    public ProfileService(ILogger<ProfileService> logger)
    {
        _logger = logger;
        
        var appData = Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData);
        var dir = Path.Combine(appData, "SimPadKneeboard");
        Directory.CreateDirectory(dir);
        _storageFilePath = Path.Combine(dir, "profiles.json");

        LoadProfiles();
    }

    public List<Profile> GetAllProfiles()
    {
        lock (_lock)
        {
            return _profiles.Select(CloneProfile).ToList();
        }
    }

    public Profile? GetProfileById(string id)
    {
        lock (_lock)
        {
            var p = _profiles.FirstOrDefault(x => x.Id == id);
            return p != null ? CloneProfile(p) : null;
        }
    }

    public Profile GetActiveProfile()
    {
        lock (_lock)
        {
            var p = _profiles.FirstOrDefault(x => x.Id == _activeProfileId)
                    ?? _profiles.FirstOrDefault(x => x.IsActive)
                    ?? _profiles.FirstOrDefault();

            if (p == null)
            {
                p = CreateDefaultProfile();
                _profiles.Add(p);
                _activeProfileId = p.Id;
            }

            return CloneProfile(p);
        }
    }

    public Profile CreateOrUpdateProfile(Profile profile)
    {
        lock (_lock)
        {
            var existing = _profiles.FirstOrDefault(x => x.Id == profile.Id);
            if (existing != null)
            {
                existing.Name = profile.Name;
                existing.Simulator = profile.Simulator;
                existing.AircraftModule = profile.AircraftModule;
                existing.FolderSources = profile.FolderSources;
                existing.WebViewTabs = profile.WebViewTabs;
                existing.UpdatedAtUtc = DateTime.UtcNow;
            }
            else
            {
                if (string.IsNullOrEmpty(profile.Id))
                {
                    profile.Id = Guid.NewGuid().ToString();
                }
                profile.CreatedAtUtc = DateTime.UtcNow;
                profile.UpdatedAtUtc = DateTime.UtcNow;
                _profiles.Add(profile);
            }

            if (profile.IsActive)
            {
                SetActiveProfileInternal(profile.Id);
            }

            SaveProfiles();
            return CloneProfile(profile);
        }
    }

    public bool DeleteProfile(string id)
    {
        lock (_lock)
        {
            var p = _profiles.FirstOrDefault(x => x.Id == id);
            if (p == null) return false;

            _profiles.Remove(p);
            if (_activeProfileId == id && _profiles.Count > 0)
            {
                SetActiveProfileInternal(_profiles[0].Id);
            }

            SaveProfiles();
            return true;
        }
    }

    public bool SetActiveProfile(string id)
    {
        lock (_lock)
        {
            var res = SetActiveProfileInternal(id);
            if (res) SaveProfiles();
            return res;
        }
    }

    private bool SetActiveProfileInternal(string id)
    {
        var target = _profiles.FirstOrDefault(x => x.Id == id);
        if (target == null) return false;

        foreach (var p in _profiles)
        {
            p.IsActive = (p.Id == id);
        }
        _activeProfileId = id;
        return true;
    }

    private void LoadProfiles()
    {
        try
        {
            if (File.Exists(_storageFilePath))
            {
                var json = File.ReadAllText(_storageFilePath);
                var list = JsonSerializer.Deserialize<List<Profile>>(json);
                if (list != null && list.Count > 0)
                {
                    _profiles.Clear();
                    _profiles.AddRange(list);
                    var active = _profiles.FirstOrDefault(x => x.IsActive) ?? _profiles[0];
                    _activeProfileId = active.Id;
                    return;
                }
            }
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "Failed to load profiles from {Path}, creating defaults", _storageFilePath);
        }

        // Initialize default seed profiles
        _profiles.Clear();
        var defaultProfile = CreateDefaultProfile();
        _profiles.Add(defaultProfile);
        _profiles.Add(CreateDcsHornetProfile());
        _profiles.Add(CreateFalconBmsProfile());
        _activeProfileId = defaultProfile.Id;
        SaveProfiles();
    }

    private void SaveProfiles()
    {
        try
        {
            var json = JsonSerializer.Serialize(_profiles, new JsonSerializerOptions { WriteIndented = true });
            File.WriteAllText(_storageFilePath, json);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to persist profiles to {Path}", _storageFilePath);
        }
    }

    private static Profile CreateDefaultProfile()
    {
        var userDocs = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
        var globalKneeboardPath = Path.Combine(userDocs, "Saved Games", "SimPadKneeboard", "Global");
        
        return new Profile
        {
            Id = "default-all-sims",
            Name = "Global / All Simulators",
            Simulator = "All",
            IsActive = true,
            FolderSources = new List<CustomFolderSource>
            {
                new()
                {
                    TabName = "Global Checklists",
                    Path = globalKneeboardPath,
                    Recursive = false,
                    Priority = 10,
                    Enabled = true
                }
            },
            WebViewTabs = new List<CustomWebViewTab>
            {
                new()
                {
                    TabName = "DCS Live Map",
                    Url = "http://localhost:8080/live-map",
                    Priority = 50,
                    Enabled = true
                }
            }
        };
    }

    private static Profile CreateDcsHornetProfile()
    {
        var userDocs = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
        var hornetPath = Path.Combine(userDocs, "Saved Games", "DCS", "KNEEBOARD", "FA-18C_hornet");

        return new Profile
        {
            Id = "dcs-fa18c",
            Name = "DCS: F/A-18C Hornet",
            Simulator = "DCS",
            AircraftModule = "FA-18C_hornet",
            IsActive = false,
            FolderSources = new List<CustomFolderSource>
            {
                new()
                {
                    TabName = "Hornet Procedures",
                    Path = hornetPath,
                    Recursive = false,
                    Priority = 20,
                    Enabled = true
                }
            }
        };
    }

    private static Profile CreateFalconBmsProfile()
    {
        var userDocs = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
        var bmsPath = Path.Combine(userDocs, "Saved Games", "Falcon BMS", "Kneeboard");

        return new Profile
        {
            Id = "falcon-bms-f16",
            Name = "Falcon BMS 4.37",
            Simulator = "FalconBMS",
            AircraftModule = "F-16C",
            IsActive = false,
            FolderSources = new List<CustomFolderSource>
            {
                new()
                {
                    TabName = "BMS F-16 Charts",
                    Path = bmsPath,
                    Recursive = false,
                    Priority = 20,
                    Enabled = true
                }
            }
        };
    }

    private static Profile CloneProfile(Profile src)
    {
        return new Profile
        {
            Id = src.Id,
            Name = src.Name,
            Simulator = src.Simulator,
            AircraftModule = src.AircraftModule,
            IsActive = src.IsActive,
            FolderSources = src.FolderSources.Select(f => new CustomFolderSource
            {
                Id = f.Id,
                TabName = f.TabName,
                Path = f.Path,
                Recursive = f.Recursive,
                Priority = f.Priority,
                Enabled = f.Enabled
            }).ToList(),
            WebViewTabs = src.WebViewTabs.Select(w => new CustomWebViewTab
            {
                Id = w.Id,
                TabName = w.TabName,
                Url = w.Url,
                Priority = w.Priority,
                Enabled = w.Enabled
            }).ToList(),
            CreatedAtUtc = src.CreatedAtUtc,
            UpdatedAtUtc = src.UpdatedAtUtc
        };
    }
}
