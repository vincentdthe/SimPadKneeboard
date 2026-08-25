using System.IO.MemoryMappedFiles;
using System.Runtime.InteropServices;
using Microsoft.Extensions.Logging;
using SimPad.Kneeboard.Server.Models;

namespace SimPad.Kneeboard.Server.Telemetry.FalconBms;

public class FalconBmsTelemetryProvider : ITelemetryProvider
{
    private readonly ILogger<FalconBmsTelemetryProvider> _logger;
    private MemoryMappedFile? _mmf1;
    private MemoryMappedFile? _mmf2;
    private MemoryMappedViewAccessor? _accessor1;
    private MemoryMappedViewAccessor? _accessor2;
    private CancellationTokenSource? _cts;
    private Task? _pollTask;
    
    private TelemetryData? _currentTelemetry;
    private bool _isConnected;
    private const string SharedMemAreaName1 = "FalconSharedMemoryArea";
    private const string SharedMemAreaName2 = "FalconSharedMemoryArea2";

    public string SimulatorName => "FalconBMS";
    public bool IsConnected => _isConnected;
    public TelemetryData? CurrentTelemetry => _currentTelemetry;

    public event EventHandler<TelemetryData>? TelemetryUpdated;
    public event EventHandler<SimStateChangedEventArgs>? SimStateChanged;

    public FalconBmsTelemetryProvider(ILogger<FalconBmsTelemetryProvider> logger)
    {
        _logger = logger;
    }

    public Task StartAsync(CancellationToken cancellationToken)
    {
        _logger.LogInformation("Starting Falcon BMS Shared Memory Provider for SimPad Server...");
        _cts = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
        _pollTask = Task.Run(() => PollLoopAsync(_cts.Token), _cts.Token);
        return Task.CompletedTask;
    }

    public async Task StopAsync(CancellationToken cancellationToken)
    {
        _logger.LogInformation("Stopping Falcon BMS Provider...");
        _cts?.Cancel();
        CloseSharedMemory();
        if (_pollTask != null)
        {
            try { await _pollTask; } catch { /* ignore */ }
        }
    }

    private async Task PollLoopAsync(CancellationToken ct)
    {
        while (!ct.IsCancellationRequested)
        {
            try
            {
                if (!EnsureConnected())
                {
                    if (_isConnected)
                    {
                        _isConnected = false;
                        _logger.LogInformation("Falcon BMS disconnected");
                        SimStateChanged?.Invoke(this, new SimStateChangedEventArgs
                        {
                            SimulatorName = "FalconBMS",
                            IsRunning = false,
                            AircraftName = "F-16C",
                            TheaterName = "Korea",
                            PreviousAircraftName = "F-16C"
                        });
                    }
                    await Task.Delay(1500, ct);
                    continue;
                }

                if (!_isConnected)
                {
                    _isConnected = true;
                    _logger.LogInformation("Falcon BMS connected via Shared Memory!");
                    SimStateChanged?.Invoke(this, new SimStateChangedEventArgs
                    {
                        SimulatorName = "FalconBMS",
                        IsRunning = true,
                        AircraftName = "F-16C",
                        TheaterName = "Korea",
                        PreviousAircraftName = null
                    });
                }

                ReadSharedMemory();
                await Task.Delay(50, ct); // 20 Hz
            }
            catch (OperationCanceledException)
            {
                break;
            }
            catch (Exception ex)
            {
                _logger.LogDebug(ex, "Error reading Falcon BMS shared memory");
                CloseSharedMemory();
                await Task.Delay(1000, ct);
            }
        }
    }

    private bool EnsureConnected()
    {
        if (!RuntimeInformation.IsOSPlatform(OSPlatform.Windows))
            return false;

        if (_accessor1 != null)
            return true;

        try
        {
            _mmf1 = MemoryMappedFile.OpenExisting(SharedMemAreaName1, MemoryMappedFileRights.Read);
            _accessor1 = _mmf1.CreateViewAccessor(0, Marshal.SizeOf<FalconFlightData>(), MemoryMappedFileAccess.Read);

            try
            {
                _mmf2 = MemoryMappedFile.OpenExisting(SharedMemAreaName2, MemoryMappedFileRights.Read);
                _accessor2 = _mmf2.CreateViewAccessor(0, Marshal.SizeOf<FalconFlightData2>(), MemoryMappedFileAccess.Read);
            }
            catch
            {
                // Area 2 optional
            }

            return true;
        }
        catch (FileNotFoundException)
        {
            CloseSharedMemory();
            return false;
        }
        catch (Exception ex)
        {
            _logger.LogDebug(ex, "Could not open Falcon BMS shared memory");
            CloseSharedMemory();
            return false;
        }
    }

    private void ReadSharedMemory()
    {
        if (_accessor1 == null) return;

        _accessor1.Read(0, out FalconFlightData fd);

        // Convert Falcon coordinates / radians to degrees and feet
        float altFeet = -fd.z;
        double pitchDeg = fd.pitch * 180.0 / Math.PI;
        double rollDeg = fd.roll * 180.0 / Math.PI;
        double headingDeg = (fd.yaw * 180.0 / Math.PI + 360.0) % 360.0;

        var telemetry = new TelemetryData
        {
            Simulator = "FalconBMS",
            Aircraft = "F-16C",
            Theater = "BMS Theater",
            MissionTitle = "Falcon BMS Flight",
            AltitudeFeet = altFeet,
            AltitudeMeters = altFeet / 3.28084,
            HeadingDeg = headingDeg,
            PitchDeg = pitchDeg,
            BankDeg = rollDeg,
            IasKnots = fd.kias,
            TasKnots = fd.vt * 0.592484,
            Mach = fd.mach,
            Status = "Active",
            LastUpdatedUtc = DateTime.UtcNow
        };

        if (_accessor2 != null)
        {
            try
            {
                _accessor2.Read(0, out FalconFlightData2 fd2);
                if (fd2.UHF_Frequency > 0)
                {
                    telemetry.Radios["UHF"] = $"{fd2.UHF_Frequency:F3} MHz (Preset {fd2.UhfPreset})";
                }
                if (fd2.VHF_Frequency > 0)
                {
                    telemetry.Radios["VHF"] = $"{fd2.VHF_Frequency:F3} MHz (Preset {fd2.VhfPreset})";
                }
                if (fd2.tacanChannel > 0)
                {
                    telemetry.Radios["TACAN"] = $"{fd2.tacanChannel:F0}X";
                }
            }
            catch
            {
                // ignore
            }
        }

        _currentTelemetry = telemetry;
        TelemetryUpdated?.Invoke(this, telemetry);
    }

    private void CloseSharedMemory()
    {
        _accessor1?.Dispose();
        _accessor1 = null;
        _mmf1?.Dispose();
        _mmf1 = null;

        _accessor2?.Dispose();
        _accessor2 = null;
        _mmf2?.Dispose();
        _mmf2 = null;
    }

    public void Dispose()
    {
        _cts?.Cancel();
        CloseSharedMemory();
    }
}
