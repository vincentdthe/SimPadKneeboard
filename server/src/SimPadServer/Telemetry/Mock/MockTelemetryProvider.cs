using Microsoft.Extensions.Logging;
using SimPad.Kneeboard.Server.Models;

namespace SimPad.Kneeboard.Server.Telemetry.Mock;

public class MockTelemetryProvider : ITelemetryProvider
{
    private readonly ILogger<MockTelemetryProvider> _logger;
    private CancellationTokenSource? _cts;
    private Task? _simTask;
    private bool _isRunning;
    private TelemetryData? _currentTelemetry;
    private string _aircraft = "FA-18C_hornet";
    private string _theater = "Caucasus";
    private double _heading = 45.0;
    private double _lat = 42.150;
    private double _lon = 41.870;
    private double _alt = 15000.0;
    private double _speed = 350.0;

    public string SimulatorName => "MockSim";
    public bool IsConnected => _isRunning;
    public TelemetryData? CurrentTelemetry => _currentTelemetry;

    public event EventHandler<TelemetryData>? TelemetryUpdated;
    public event EventHandler<SimStateChangedEventArgs>? SimStateChanged;

    public MockTelemetryProvider(ILogger<MockTelemetryProvider> logger)
    {
        _logger = logger;
    }

    public Task StartAsync(CancellationToken cancellationToken)
    {
        _logger.LogInformation("Starting Mock Telemetry Provider for SimPad...");
        _cts = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
        _isRunning = true;
        _simTask = Task.Run(() => SimulationLoopAsync(_cts.Token), _cts.Token);
        
        SimStateChanged?.Invoke(this, new SimStateChangedEventArgs
        {
            SimulatorName = "MockSim",
            IsRunning = true,
            AircraftName = _aircraft,
            TheaterName = _theater,
            PreviousAircraftName = null
        });

        return Task.CompletedTask;
    }

    public async Task StopAsync(CancellationToken cancellationToken)
    {
        _logger.LogInformation("Stopping Mock Telemetry Provider...");
        _isRunning = false;
        _cts?.Cancel();
        if (_simTask != null)
        {
            try { await _simTask; } catch { /* ignore */ }
        }

        SimStateChanged?.Invoke(this, new SimStateChangedEventArgs
        {
            SimulatorName = "MockSim",
            IsRunning = false,
            AircraftName = _aircraft,
            TheaterName = _theater,
            PreviousAircraftName = _aircraft
        });
    }

    public void SetSimulatedAircraft(string aircraft, string theater = "Caucasus")
    {
        var prev = _aircraft;
        _aircraft = aircraft;
        _theater = theater;
        _logger.LogInformation("Mock simulated aircraft changed to: {Aircraft}", aircraft);

        SimStateChanged?.Invoke(this, new SimStateChangedEventArgs
        {
            SimulatorName = "MockSim",
            IsRunning = _isRunning,
            AircraftName = _aircraft,
            TheaterName = _theater,
            PreviousAircraftName = prev
        });
    }

    private async Task SimulationLoopAsync(CancellationToken ct)
    {
        while (!ct.IsCancellationRequested && _isRunning)
        {
            _heading = (_heading + 0.1) % 360.0;
            _lat += 0.00005 * Math.Cos(_heading * Math.PI / 180.0);
            _lon += 0.00005 * Math.Sin(_heading * Math.PI / 180.0);

            var telemetry = new TelemetryData
            {
                Simulator = "MockSim",
                Aircraft = _aircraft,
                Theater = _theater,
                MissionTitle = "Training Sortie (Mock)",
                Latitude = _lat,
                Longitude = _lon,
                AltitudeFeet = _alt,
                AltitudeMeters = _alt / 3.28084,
                AglFeet = _alt - 1200.0,
                AglMeters = (_alt - 1200.0) / 3.28084,
                HeadingDeg = _heading,
                PitchDeg = 1.5,
                BankDeg = 2.0 * Math.Sin(_heading / 10.0),
                IasKnots = _speed,
                TasKnots = _speed * 1.2,
                Mach = 0.62,
                Radios = new Dictionary<string, string>
                {
                    { "PRI_COM1", "305.000 MHz (VHF/UHF)" },
                    { "AUX_COM2", "127.500 MHz (Tower)" },
                    { "TACAN", "73X (KOB)" },
                    { "ILS", "108.90 MHz (RWY 07)" }
                },
                Bullseye = new BullseyeInfo
                {
                    BearingDeg = 180.0,
                    DistanceNm = 32.5
                },
                Status = "Active",
                LastUpdatedUtc = DateTime.UtcNow
            };

            _currentTelemetry = telemetry;
            TelemetryUpdated?.Invoke(this, telemetry);

            await Task.Delay(50, ct); // 20 Hz
        }
    }

    public void Dispose()
    {
        _cts?.Cancel();
    }
}
