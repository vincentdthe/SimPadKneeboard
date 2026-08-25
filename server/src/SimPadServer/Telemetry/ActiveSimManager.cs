using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;
using SimPad.Kneeboard.Server.Models;
using SimPad.Kneeboard.Server.Telemetry.Dcs;
using SimPad.Kneeboard.Server.Telemetry.FalconBms;
using SimPad.Kneeboard.Server.Telemetry.Mock;

namespace SimPad.Kneeboard.Server.Telemetry;

public class ActiveSimManager : IHostedService
{
    private readonly ILogger<ActiveSimManager> _logger;
    private readonly List<ITelemetryProvider> _providers = new();
    private ITelemetryProvider? _activeProvider;
    private readonly object _lock = new();

    public DcsTelemetryProvider DcsProvider { get; }
    public FalconBmsTelemetryProvider FalconProvider { get; }
    public MockTelemetryProvider MockProvider { get; }

    public string ActiveSimulator => _activeProvider?.SimulatorName ?? "None";
    public string ActiveAircraft => _activeProvider?.CurrentTelemetry?.Aircraft ?? "None";
    public string ActiveTheater => _activeProvider?.CurrentTelemetry?.Theater ?? "None";
    public TelemetryData? CurrentTelemetry => _activeProvider?.CurrentTelemetry;

    public event EventHandler<TelemetryData>? TelemetryUpdated;
    public event EventHandler<SimStateChangedEventArgs>? SimStateChanged;

    public ActiveSimManager(
        ILogger<ActiveSimManager> logger,
        DcsTelemetryProvider dcsProvider,
        FalconBmsTelemetryProvider falconProvider,
        MockTelemetryProvider mockProvider)
    {
        _logger = logger;
        DcsProvider = dcsProvider;
        FalconProvider = falconProvider;
        MockProvider = mockProvider;

        RegisterProvider(dcsProvider);
        RegisterProvider(falconProvider);
        RegisterProvider(mockProvider);
    }

    private void RegisterProvider(ITelemetryProvider provider)
    {
        _providers.Add(provider);
        provider.TelemetryUpdated += (sender, data) =>
        {
            if (ReferenceEquals(_activeProvider, provider))
            {
                TelemetryUpdated?.Invoke(this, data);
            }
        };

        provider.SimStateChanged += (sender, args) =>
        {
            lock (_lock)
            {
                EvaluateActiveProvider();
            }
            SimStateChanged?.Invoke(this, args);
        };
    }

    public async Task StartAsync(CancellationToken cancellationToken)
    {
        _logger.LogInformation("Starting SimPad ActiveSimManager and telemetry providers...");
        await DcsProvider.StartAsync(cancellationToken);
        await FalconProvider.StartAsync(cancellationToken);
    }

    public async Task StopAsync(CancellationToken cancellationToken)
    {
        _logger.LogInformation("Stopping SimPad ActiveSimManager...");
        foreach (var provider in _providers)
        {
            await provider.StopAsync(cancellationToken);
        }
    }

    public void EnableMockTelemetry(bool enable, string aircraft = "FA-18C_hornet")
    {
        if (enable)
        {
            MockProvider.SetSimulatedAircraft(aircraft);
            _ = MockProvider.StartAsync(CancellationToken.None);
            lock (_lock)
            {
                _activeProvider = MockProvider;
            }
        }
        else
        {
            _ = MockProvider.StopAsync(CancellationToken.None);
            lock (_lock)
            {
                EvaluateActiveProvider();
            }
        }
    }

    private void EvaluateActiveProvider()
    {
        if (DcsProvider.IsConnected)
        {
            _activeProvider = DcsProvider;
        }
        else if (FalconProvider.IsConnected)
        {
            _activeProvider = FalconProvider;
        }
        else if (MockProvider.IsConnected)
        {
            _activeProvider = MockProvider;
        }
        else
        {
            _activeProvider = null;
        }

        _logger.LogInformation("Active simulator evaluated: {Sim}", ActiveSimulator);
    }
}
