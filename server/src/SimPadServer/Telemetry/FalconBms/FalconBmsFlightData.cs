using System.Runtime.InteropServices;

namespace SimPad.Kneeboard.Server.Telemetry.FalconBms;

[StructLayout(LayoutKind.Sequential, Pack = 4)]
public struct FalconFlightData
{
    public float x;            // North/South (feet)
    public float y;            // East/West (feet)
    public float z;            // -Altitude (feet)
    public float xDot;
    public float yDot;
    public float zDot;
    public float alpha;        // AoA (degrees)
    public float beta;         // Sideslip (degrees)
    public float gamma;        // Flight path angle (radians)
    public float pitch;        // Radians
    public float roll;         // Radians
    public float yaw;          // Radians
    public float mach;
    public float kias;         // Knots Indicated Airspeed
    public float vt;           // True Airspeed (ft/sec)
    public float gs;           // G-load (normal)
    public float windSpeed;
    public float windHeading;
    public float nozzlePos;
    public float nozzlePos2;
    public float internalFuel;
    public float externalFuel;
    public float fuelJoker;
    public float fuelBingo;
    public float fuelFwd;
    public float fuelAft;
    public float rpm;
    public float rpm2;
    public float ftit;
    public float ftit2;
    public float oilPressure;
    public float oilPressure2;
    public float hydPressureA;
    public float hydPressureB;
    public uint lightBits;
    public uint lightBits2;
    public uint lightBits3;
    public int versionNum;
    public float headPitch;
    public float headRoll;
    public float headYaw;
    public uint lightBits4;
    public uint lightBits5;
    public uint lightBits6;
    public float speedBrake;
    public float epuFuel;
    public float gearPos;
    public float cabinAlt;
    public float hydPressureMaster;
    public float setRoll;
    public float setPitch;
    public float setYaw;
    public float tacanChannel;
    public uint navMode;
}

[StructLayout(LayoutKind.Sequential, Pack = 4)]
public struct FalconFlightData2
{
    public float nozzlePos;
    public float nozzlePos2;
    public float rpm;
    public float rpm2;
    public float ftit;
    public float ftit2;
    public float oilPressure;
    public float oilPressure2;
    public float hydPressureA;
    public float hydPressureB;
    public uint lightBits;
    public uint lightBits2;
    public uint lightBits3;
    public float UHF_Frequency;
    public float VHF_Frequency;
    public int UhfPreset;
    public int VhfPreset;
    public int backupUhfPreset;
    public float centerAlt;
    public float tacanChannel;
    public int navMode;
}
