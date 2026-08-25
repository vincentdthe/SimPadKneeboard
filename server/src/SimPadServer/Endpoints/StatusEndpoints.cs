using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Routing;
using SimPad.Kneeboard.Server.Models;
using SimPad.Kneeboard.Server.Services;
using SimPad.Kneeboard.Server.Telemetry;
using SimPad.Kneeboard.Server.WebSockets;

namespace SimPad.Kneeboard.Server.Endpoints;

public static class StatusEndpoints
{
    public static DateTime ServerStartTimeUtc { get; set; } = DateTime.UtcNow;

    public static IEndpointRouteBuilder MapStatusEndpoints(this IEndpointRouteBuilder routes)
    {
        routes.MapGet("/api/status", (
            ActiveSimManager simManager,
            IProfileService profileService,
            TelemetryWebSocketManager wsManager) =>
        {
            var activeProfile = profileService.GetActiveProfile();
            var status = new ServerStatus
            {
                ServerName = "SimPad Kneeboard Server",
                Version = "1.0.0",
                ActiveSimulator = simManager.ActiveSimulator,
                ActiveAircraft = simManager.ActiveAircraft,
                ActiveTheater = simManager.ActiveTheater,
                ActiveProfileId = activeProfile.Id,
                ActiveProfileName = activeProfile.Name,
                ConnectedClientsCount = wsManager.ConnectedClientsCount,
                StartTimeUtc = ServerStartTimeUtc,
            };
            return Results.Ok(status);
        })
        .WithName("GetServerStatus")
        .WithSummary("Get server health, active simulator, and connected clients status.");

        routes.MapGet("/api/health", () => Results.Ok(new { Status = "Healthy", UtcTime = DateTime.UtcNow }))
        .WithName("GetHealth");

        return routes;
    }
}
