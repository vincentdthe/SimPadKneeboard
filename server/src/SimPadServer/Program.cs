using SimPad.Kneeboard.Server.Endpoints;
using SimPad.Kneeboard.Server.Services;
using SimPad.Kneeboard.Server.Telemetry;
using SimPad.Kneeboard.Server.Telemetry.Dcs;
using SimPad.Kneeboard.Server.Telemetry.FalconBms;
using SimPad.Kneeboard.Server.Telemetry.Mock;
using SimPad.Kneeboard.Server.WebSockets;

var builder = WebApplication.CreateBuilder(args);

// Configure Kestrel to bind to all network interfaces so Android tablets on LAN can connect
builder.WebHost.ConfigureKestrel(options =>
{
    var port = builder.Configuration.GetValue<int>("Server:Port", 8090);
    options.ListenAnyIP(port);
});

// Configure CORS for mobile / tablet clients on local network
builder.Services.AddCors(options =>
{
    options.AddDefaultPolicy(policy =>
    {
        policy.AllowAnyOrigin()
              .AllowAnyMethod()
              .AllowAnyHeader();
    });
});

// Register Telemetry Providers & Core Sim Services
builder.Services.AddSingleton<DcsTelemetryProvider>(sp =>
{
    var logger = sp.GetRequiredService<ILogger<DcsTelemetryProvider>>();
    var dcsPort = builder.Configuration.GetValue<int>("DCS:UdpPort", 17290);
    return new DcsTelemetryProvider(logger, dcsPort);
});
builder.Services.AddSingleton<FalconBmsTelemetryProvider>();
builder.Services.AddSingleton<MockTelemetryProvider>();
builder.Services.AddSingleton<ActiveSimManager>();
builder.Services.AddHostedService(sp => sp.GetRequiredService<ActiveSimManager>());

// Register Document, Profile, and Hierarchy Services
builder.Services.AddSingleton<IDocumentService, DocumentService>();
builder.Services.AddSingleton<IProfileService, ProfileService>();
builder.Services.AddSingleton<IKneeboardHierarchyService, KneeboardHierarchyService>();

// Register WebSocket Telemetry Manager
builder.Services.AddSingleton<TelemetryWebSocketManager>();

var app = builder.Build();

app.UseCors();
app.UseWebSockets(new WebSocketOptions
{
    KeepAliveInterval = TimeSpan.FromSeconds(15)
});

// WebSocket Telemetry Channel
app.Map("/ws/telemetry", async (HttpContext context, TelemetryWebSocketManager wsManager) =>
{
    if (context.WebSockets.IsWebSocketRequest)
    {
        using var webSocket = await context.WebSockets.AcceptWebSocketAsync();
        await wsManager.HandleWebSocketAsync(webSocket);
    }
    else
    {
        context.Response.StatusCode = StatusCodes.Status400BadRequest;
    }
});

// Map REST Endpoint Groups
app.MapStatusEndpoints();
app.MapTelemetryEndpoints();
app.MapProfileEndpoints();
app.MapKneeboardEndpoints();
app.MapSimEndpoints();

// Root landing endpoint
app.MapGet("/", () => Results.Ok(new
{
    Name = "SimPad Kneeboard Server",
    Status = "Online",
    ApiDocs = "/api/status",
    WebSocket = "/ws/telemetry"
}));

Console.WriteLine("===============================================================");
Console.WriteLine("  SimPad Kneeboard Server Started");
Console.WriteLine("  Listening for Android Tablets on http://0.0.0.0:8090");
Console.WriteLine("  DCS World UDP Telemetry Listener: Port 17290");
Console.WriteLine("  Falcon BMS Shared Memory: Active");
Console.WriteLine("===============================================================");

app.Run();
