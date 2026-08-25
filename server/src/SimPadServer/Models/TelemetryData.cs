namespace SimPad.Kneeboard.Server.Models;

public class TelemetryData
{
    public string Simulator { get; set; } = "None"; // "DCS", "FalconBMS", "MSFS", "MockSim"
    public string Aircraft { get; set; } = "Unknown";
    public string Theater { get; set; } = "Unknown";
    public string MissionTitle { get; set; } = "";
    
    // Position
    public double Latitude { get; set; }
    public double Longitude { get; set; }
    public double AltitudeMeters { get; set; }
    public double AltitudeFeet { get; set; }
    public double AglMeters { get; set; }
    public double AglFeet { get; set; }

    // Attitude
    public double HeadingDeg { get; set; }
    public double PitchDeg { get; set; }
    public double BankDeg { get; set; }

    // Airspeed
    public double IasKnots { get; set; }
    public double TasKnots { get; set; }
    public double Mach { get; set; }

    // Radios
    public Dictionary<string, string> Radios { get; set; } = new();

    // Bullseye
    public BullseyeInfo? Bullseye { get; set; }

    // System Status
    public string Status { get; set; } = "Disconnected"; // "Active", "Paused", "Disconnected"
    public DateTime LastUpdatedUtc { get; set; } = DateTime.UtcNow;
}

public class BullseyeInfo
{
    public double BearingDeg { get; set; }
    public double DistanceNm { get; set; }
}

public class SimStateChangedEventArgs : EventArgs
{
    public string SimulatorName { get; set; } = "";
    public bool IsRunning { get; set; }
    public string AircraftName { get; set; } = "";
    public string TheaterName { get; set; } = "";
    public string? PreviousAircraftName { get; set; }
}
