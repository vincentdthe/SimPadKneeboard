using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Routing;
using SimPad.Kneeboard.Server.Models;
using SimPad.Kneeboard.Server.Telemetry;

namespace SimPad.Kneeboard.Server.Endpoints;

public record MockConfigRequest(bool Enable, string? Aircraft, string? Theater);

public static class TelemetryEndpoints
{
    public static IEndpointRouteBuilder MapTelemetryEndpoints(this IEndpointRouteBuilder routes)
    {
        var group = routes.MapGroup("/api/telemetry");

        group.MapGet("/", (ActiveSimManager simManager) =>
        {
            var telemetry = simManager.CurrentTelemetry ?? new TelemetryData
            {
                Simulator = "None",
                Status = "Disconnected",
                LastUpdatedUtc = DateTime.UtcNow
            };
            return Results.Ok(telemetry);
        })
        .WithName("GetCurrentTelemetry")
        .WithSummary("Get the latest telemetry frame from the active simulator.");

        group.MapPost("/mock", (ActiveSimManager simManager, MockConfigRequest request) =>
        {
            simManager.EnableMockTelemetry(request.Enable, request.Aircraft ?? "FA-18C_hornet");
            return Results.Ok(new
            {
                Success = true,
                MockEnabled = request.Enable,
                Aircraft = request.Aircraft ?? "FA-18C_hornet"
            });
        })
        .WithName("ConfigureMockTelemetry")
        .WithSummary("Enable or disable mock telemetry stream for testing.");

        return routes;
    }
}
