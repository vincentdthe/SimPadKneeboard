using SimPad.Kneeboard.Server.Models;

namespace SimPad.Kneeboard.Server.Telemetry;

public interface ITelemetryProvider : IDisposable
{
    string SimulatorName { get; }
    bool IsConnected { get; }
    TelemetryData? CurrentTelemetry { get; }
    
    event EventHandler<TelemetryData>? TelemetryUpdated;
    event EventHandler<SimStateChangedEventArgs>? SimStateChanged;
    
    Task StartAsync(CancellationToken cancellationToken);
    Task StopAsync(CancellationToken cancellationToken);
}
