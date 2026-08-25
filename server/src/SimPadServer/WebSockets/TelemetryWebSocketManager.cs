using System.Collections.Concurrent;
using System.Net.WebSockets;
using System.Text;
using System.Text.Json;
using Microsoft.Extensions.Logging;
using SimPad.Kneeboard.Server.Models;
using SimPad.Kneeboard.Server.Services;
using SimPad.Kneeboard.Server.Telemetry;

namespace SimPad.Kneeboard.Server.WebSockets;

public class TelemetryWebSocketManager : IDisposable
{
    private readonly ILogger<TelemetryWebSocketManager> _logger;
    private readonly ActiveSimManager _activeSimManager;
    private readonly IKneeboardHierarchyService _hierarchyService;
    private readonly ConcurrentDictionary<string, WebSocket> _sockets = new();
    private readonly JsonSerializerOptions _jsonOptions = new() { PropertyNamingPolicy = JsonNamingPolicy.CamelCase };
    private DateTime _lastBroadcastUtc = DateTime.MinValue;
    private readonly TimeSpan _broadcastThrottle = TimeSpan.FromMilliseconds(50); // 20 Hz max

    public int ConnectedClientsCount => _sockets.Count;

    public TelemetryWebSocketManager(
        ILogger<TelemetryWebSocketManager> logger,
        ActiveSimManager activeSimManager,
        IKneeboardHierarchyService hierarchyService)
    {
        _logger = logger;
        _activeSimManager = activeSimManager;
        _hierarchyService = hierarchyService;

        _activeSimManager.TelemetryUpdated += OnTelemetryUpdated;
        _activeSimManager.SimStateChanged += OnSimStateChanged;
        _hierarchyService.TabsInvalidated += OnTabsInvalidated;
    }

    public async Task HandleWebSocketAsync(WebSocket socket)
    {
        var connectionId = Guid.NewGuid().ToString();
        _sockets.TryAdd(connectionId, socket);
        _logger.LogInformation("SimPad Android Client connected (WS ID: {Id}). Total clients: {Count}", connectionId, _sockets.Count);

        // Send initial connection greeting with current status
        var initialMsg = new WebSocketMessage
        {
            Type = "connected",
            Payload = new
            {
                ConnectionId = connectionId,
                Simulator = _activeSimManager.ActiveSimulator,
                Aircraft = _activeSimManager.ActiveAircraft,
                CurrentTelemetry = _activeSimManager.CurrentTelemetry
            }
        };
        await SendMessageAsync(socket, initialMsg);

        var buffer = new byte[4096];
        try
        {
            while (socket.State == WebSocketState.Open)
            {
                var result = await socket.ReceiveAsync(new ArraySegment<byte>(buffer), CancellationToken.None);
                if (result.MessageType == WebSocketMessageType.Close)
                {
                    await socket.CloseAsync(WebSocketCloseStatus.NormalClosure, "Closed by client", CancellationToken.None);
                    break;
                }

                if (result.MessageType == WebSocketMessageType.Text)
                {
                    var msgText = Encoding.UTF8.GetString(buffer, 0, result.Count);
                    await ProcessClientMessageAsync(socket, msgText);
                }
            }
        }
        catch (Exception ex)
        {
            _logger.LogDebug(ex, "WebSocket client exception (ID: {Id})", connectionId);
        }
        finally
        {
            _sockets.TryRemove(connectionId, out _);
            _logger.LogInformation("SimPad Android Client disconnected (WS ID: {Id}). Total clients: {Count}", connectionId, _sockets.Count);
        }
    }

    private async Task ProcessClientMessageAsync(WebSocket socket, string msgText)
    {
        try
        {
            using var doc = JsonDocument.Parse(msgText);
            var root = doc.RootElement;
            if (root.TryGetProperty("type", out var typeProp))
            {
                var type = typeProp.GetString();
                if (type == "ping")
                {
                    await SendMessageAsync(socket, new WebSocketMessage { Type = "pong" });
                }
                else if (type == "request_refresh")
                {
                    await SendMessageAsync(socket, new WebSocketMessage
                    {
                        Type = "tabs_invalidated",
                        Payload = new { Timestamp = DateTime.UtcNow }
                    });
                }
            }
        }
        catch (Exception ex)
        {
            _logger.LogDebug(ex, "Failed to parse client WS message: {Msg}", msgText);
        }
    }

    private void OnTelemetryUpdated(object? sender, TelemetryData telemetry)
    {
        if (_sockets.IsEmpty) return;

        var now = DateTime.UtcNow;
        if (now - _lastBroadcastUtc < _broadcastThrottle)
            return;

        _lastBroadcastUtc = now;

        var msg = new WebSocketMessage
        {
            Type = "telemetry_frame",
            Payload = telemetry
        };

        _ = BroadcastMessageAsync(msg);
    }

    private void OnSimStateChanged(object? sender, SimStateChangedEventArgs args)
    {
        _logger.LogInformation("Broadcasting SimStateChanged to {Count} clients: Sim={Sim}, Aircraft={Aircraft}", 
            _sockets.Count, args.SimulatorName, args.AircraftName);

        var msg = new WebSocketMessage
        {
            Type = "sim_state_changed",
            Payload = args
        };

        _ = BroadcastMessageAsync(msg);
    }

    private void OnTabsInvalidated(object? sender, EventArgs e)
    {
        var msg = new WebSocketMessage
        {
            Type = "tabs_invalidated",
            Payload = new { Message = "Active simulator or module changed. Reload tabs." }
        };

        _ = BroadcastMessageAsync(msg);
    }

    private async Task BroadcastMessageAsync(WebSocketMessage msg)
    {
        var bytes = Encoding.UTF8.GetBytes(JsonSerializer.Serialize(msg, _jsonOptions));
        var segment = new ArraySegment<byte>(bytes);

        foreach (var (id, socket) in _sockets)
        {
            if (socket.State == WebSocketState.Open)
            {
                try
                {
                    await socket.SendAsync(segment, WebSocketMessageType.Text, true, CancellationToken.None);
                }
                catch
                {
                    // Socket closing
                }
            }
        }
    }

    private async Task SendMessageAsync(WebSocket socket, WebSocketMessage msg)
    {
        if (socket.State != WebSocketState.Open) return;
        var bytes = Encoding.UTF8.GetBytes(JsonSerializer.Serialize(msg, _jsonOptions));
        await socket.SendAsync(new ArraySegment<byte>(bytes), WebSocketMessageType.Text, true, CancellationToken.None);
    }

    public void Dispose()
    {
        _activeSimManager.TelemetryUpdated -= OnTelemetryUpdated;
        _activeSimManager.SimStateChanged -= OnSimStateChanged;
        _hierarchyService.TabsInvalidated -= OnTabsInvalidated;

        foreach (var (_, socket) in _sockets)
        {
            try
            {
                socket.Dispose();
            }
            catch { /* ignore */ }
        }
        _sockets.Clear();
    }
}
