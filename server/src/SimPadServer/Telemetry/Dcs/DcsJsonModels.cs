using System.Text.Json.Serialization;

namespace SimPad.Kneeboard.Server.Telemetry.Dcs;

public class DcsPacket
{
    [JsonPropertyName("sim")]
    public string? Sim { get; set; }

    [JsonPropertyName("event")]
    public string? Event { get; set; }

    [JsonPropertyName("timestamp")]
    public double Timestamp { get; set; }

    [JsonPropertyName("aircraft")]
    public string? Aircraft { get; set; }

    [JsonPropertyName("theater")]
    public string? Theater { get; set; }

    [JsonPropertyName("mission_title")]
    public string? MissionTitle { get; set; }

    [JsonPropertyName("coordinates")]
    public DcsCoordinates? Coordinates { get; set; }

    [JsonPropertyName("attitude")]
    public DcsAttitude? Attitude { get; set; }

    [JsonPropertyName("airspeed")]
    public DcsAirspeed? Airspeed { get; set; }

    [JsonPropertyName("radios")]
    public Dictionary<string, object>? Radios { get; set; }

    [JsonPropertyName("install_path")]
    public string? InstallPath { get; set; }

    [JsonPropertyName("saved_games_path")]
    public string? SavedGamesPath { get; set; }
}

public class DcsCoordinates
{
    [JsonPropertyName("latitude")]
    public double Latitude { get; set; }

    [JsonPropertyName("longitude")]
    public double Longitude { get; set; }

    [JsonPropertyName("altitude_meters")]
    public double AltitudeMeters { get; set; }

    [JsonPropertyName("altitude_feet")]
    public double AltitudeFeet { get; set; }

    [JsonPropertyName("agl_meters")]
    public double AglMeters { get; set; }

    [JsonPropertyName("agl_feet")]
    public double AglFeet { get; set; }
}

public class DcsAttitude
{
    [JsonPropertyName("heading_deg")]
    public double HeadingDeg { get; set; }

    [JsonPropertyName("pitch_deg")]
    public double PitchDeg { get; set; }

    [JsonPropertyName("bank_deg")]
    public double BankDeg { get; set; }
}

public class DcsAirspeed
{
    [JsonPropertyName("ias_knots")]
    public double IasKnots { get; set; }

    [JsonPropertyName("tas_knots")]
    public double TasKnots { get; set; }

    [JsonPropertyName("mach")]
    public double Mach { get; set; }
}
