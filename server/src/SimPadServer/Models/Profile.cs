namespace SimPad.Kneeboard.Server.Models;

public class Profile
{
    public string Id { get; set; } = Guid.NewGuid().ToString();
    public string Name { get; set; } = "Default Profile";
    public string Simulator { get; set; } = "All"; // "All", "DCS", "FalconBMS", "MSFS"
    public string? AircraftModule { get; set; } // e.g. "FA-18C_hornet", "F-16C_50", null = all
    public bool IsActive { get; set; }
    
    // Custom folder locations prioritized by the user
    public List<CustomFolderSource> FolderSources { get; set; } = new();
    
    // Custom WebView tabs (VAICOM PRO, DCS Web Editor Live Map, etc.)
    public List<CustomWebViewTab> WebViewTabs { get; set; } = new();

    public DateTime CreatedAtUtc { get; set; } = DateTime.UtcNow;
    public DateTime UpdatedAtUtc { get; set; } = DateTime.UtcNow;
}

public class CustomFolderSource
{
    public string Id { get; set; } = Guid.NewGuid().ToString();
    public string TabName { get; set; } = "";
    public string Path { get; set; } = "";
    public bool Recursive { get; set; }
    public int Priority { get; set; }
    public bool Enabled { get; set; } = true;
}

public class CustomWebViewTab
{
    public string Id { get; set; } = Guid.NewGuid().ToString();
    public string TabName { get; set; } = "";
    public string Url { get; set; } = "";
    public int Priority { get; set; }
    public bool Enabled { get; set; } = true;
}

public class ServerStatus
{
    public string ServerName { get; set; } = "SimPad Kneeboard Server";
    public string Version { get; set; } = "1.0.0";
    public int HttpPort { get; set; }
    public int DcsUdpPort { get; set; } = 17290;
    public string ActiveSimulator { get; set; } = "None";
    public string ActiveAircraft { get; set; } = "None";
    public string ActiveTheater { get; set; } = "None";
    public string ActiveProfileId { get; set; } = "";
    public string ActiveProfileName { get; set; } = "";
    public int ConnectedClientsCount { get; set; }
    public DateTime StartTimeUtc { get; set; }
    public double UptimeSeconds => (DateTime.UtcNow - StartTimeUtc).TotalSeconds;
}

public class WebSocketMessage
{
    public string Type { get; set; } = ""; // "telemetry_frame", "sim_state_changed", "tabs_invalidated", "pong", "error"
    public object? Payload { get; set; }
    public DateTime TimestampUtc { get; set; } = DateTime.UtcNow;
}
