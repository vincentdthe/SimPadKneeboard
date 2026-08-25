using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Text.Json;
using Microsoft.Extensions.Logging;
using SimPad.Kneeboard.Server.Models;

namespace SimPad.Kneeboard.Server.Telemetry.Dcs;

public class DcsTelemetryProvider : ITelemetryProvider
{
    private readonly ILogger<DcsTelemetryProvider> _logger;
    private readonly int _port;
    private UdpClient? _udpClient;
    private CancellationTokenSource? _cts;
    private Task? _listenerTask;
    private Task? _watchdogTask;
    private DateTime _lastPacketTimeUtc = DateTime.MinValue;
    private readonly TimeSpan _timeout = TimeSpan.FromSeconds(4);
    
    private TelemetryData? _currentTelemetry;
    private bool _isConnected;
    private string _currentAircraft = "";
    private string _currentTheater = "";

    public string SimulatorName => "DCS";
    public bool IsConnected => _isConnected;
    public TelemetryData? CurrentTelemetry => _currentTelemetry;

    public event EventHandler<TelemetryData>? TelemetryUpdated;
    public event EventHandler<SimStateChangedEventArgs>? SimStateChanged;

    public DcsTelemetryProvider(ILogger<DcsTelemetryProvider> logger, int port = 17290)
    {
        _logger = logger;
        _port = port;
    }

    public Task StartAsync(CancellationToken cancellationToken)
    {
        _logger.LogInformation("Starting SimPad DCS Telemetry Provider UDP listener on 127.0.0.1:{Port}...", _port);
        _cts = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
        
        try
        {
            _udpClient = new UdpClient(new IPEndPoint(IPAddress.Any, _port));
            _listenerTask = Task.Run(() => ListenLoopAsync(_cts.Token), _cts.Token);
            _watchdogTask = Task.Run(() => WatchdogLoopAsync(_cts.Token), _cts.Token);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Failed to bind DCS UDP socket on port {Port}", _port);
        }

        return Task.CompletedTask;
    }

    public async Task StopAsync(CancellationToken cancellationToken)
    {
        _logger.LogInformation("Stopping DCS Telemetry Provider...");
        _cts?.Cancel();
        _udpClient?.Close();
        
        if (_listenerTask != null)
        {
            try { await _listenerTask; } catch { /* ignore */ }
        }
        if (_watchdogTask != null)
        {
            try { await _watchdogTask; } catch { /* ignore */ }
        }
    }

    private async Task ListenLoopAsync(CancellationToken ct)
    {
        while (!ct.IsCancellationRequested && _udpClient != null)
        {
            try
            {
                var result = await _udpClient.ReceiveAsync(ct);
                var json = Encoding.UTF8.GetString(result.Buffer);
                ProcessPacket(json);
            }
            catch (OperationCanceledException)
            {
                break;
            }
            catch (Exception ex)
            {
                if (!ct.IsCancellationRequested)
                {
                    _logger.LogWarning(ex, "Error processing incoming DCS UDP packet");
                }
            }
        }
    }

    private void ProcessPacket(string json)
    {
        try
        {
            var packet = JsonSerializer.Deserialize<DcsPacket>(json);
            if (packet == null) return;

            _lastPacketTimeUtc = DateTime.UtcNow;

            if (!_isConnected)
            {
                _isConnected = true;
                _logger.LogInformation("DCS World connected to SimPad Server!");
                SimStateChanged?.Invoke(this, new SimStateChangedEventArgs
                {
                    SimulatorName = "DCS",
                    IsRunning = true,
                    AircraftName = packet.Aircraft ?? _currentAircraft,
                    TheaterName = packet.Theater ?? _currentTheater,
                    PreviousAircraftName = null
                });
            }

            // Detect aircraft module or theater change
            if (!string.IsNullOrEmpty(packet.Aircraft) && packet.Aircraft != _currentAircraft)
            {
                var prev = _currentAircraft;
                _currentAircraft = packet.Aircraft;
                _logger.LogInformation("DCS Aircraft changed: {Prev} -> {New}", prev, _currentAircraft);
                
                SimStateChanged?.Invoke(this, new SimStateChangedEventArgs
                {
                    SimulatorName = "DCS",
                    IsRunning = true,
                    AircraftName = _currentAircraft,
                    TheaterName = packet.Theater ?? _currentTheater,
                    PreviousAircraftName = prev
                });
            }

            if (!string.IsNullOrEmpty(packet.Theater))
            {
                _currentTheater = packet.Theater;
            }

            var telemetry = new TelemetryData
            {
                Simulator = "DCS",
                Aircraft = _currentAircraft,
                Theater = _currentTheater,
                MissionTitle = packet.MissionTitle ?? "",
                Latitude = packet.Coordinates?.Latitude ?? 0,
                Longitude = packet.Coordinates?.Longitude ?? 0,
                AltitudeMeters = packet.Coordinates?.AltitudeMeters ?? 0,
                AltitudeFeet = packet.Coordinates?.AltitudeFeet ?? 0,
                AglMeters = packet.Coordinates?.AglMeters ?? 0,
                AglFeet = packet.Coordinates?.AglFeet ?? 0,
                HeadingDeg = packet.Attitude?.HeadingDeg ?? 0,
                PitchDeg = packet.Attitude?.PitchDeg ?? 0,
                BankDeg = packet.Attitude?.BankDeg ?? 0,
                IasKnots = packet.Airspeed?.IasKnots ?? 0,
                TasKnots = packet.Airspeed?.TasKnots ?? 0,
                Mach = packet.Airspeed?.Mach ?? 0,
                Status = "Active",
                LastUpdatedUtc = DateTime.UtcNow
            };

            if (packet.Radios != null)
            {
                foreach (var kvp in packet.Radios)
                {
                    telemetry.Radios[kvp.Key] = kvp.Value?.ToString() ?? "";
                }
            }

            _currentTelemetry = telemetry;
            TelemetryUpdated?.Invoke(this, telemetry);
        }
        catch (Exception ex)
        {
            _logger.LogDebug(ex, "Failed to parse DCS packet: {Json}", json);
        }
    }

    private async Task WatchdogLoopAsync(CancellationToken ct)
    {
        while (!ct.IsCancellationRequested)
        {
            await Task.Delay(1000, ct);

            if (_isConnected && (DateTime.UtcNow - _lastPacketTimeUtc) > _timeout)
            {
                _isConnected = false;
                _logger.LogInformation("DCS World connection lost (watchdog timeout)");
                
                if (_currentTelemetry != null)
                {
                    _currentTelemetry.Status = "Disconnected";
                }

                SimStateChanged?.Invoke(this, new SimStateChangedEventArgs
                {
                    SimulatorName = "DCS",
                    IsRunning = false,
                    AircraftName = _currentAircraft,
                    TheaterName = _currentTheater,
                    PreviousAircraftName = _currentAircraft
                });
            }
        }
    }

    public void Dispose()
    {
        _cts?.Cancel();
        _udpClient?.Dispose();
    }
}
