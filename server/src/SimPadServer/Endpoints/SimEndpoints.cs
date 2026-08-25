using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Routing;
using SimPad.Kneeboard.Server.Telemetry;

namespace SimPad.Kneeboard.Server.Endpoints;

public static class SimEndpoints
{
    public static IEndpointRouteBuilder MapSimEndpoints(this IEndpointRouteBuilder routes)
    {
        var group = routes.MapGroup("/api/sims");

        group.MapGet("/dcs/hook-script", () =>
        {
            var searchPaths = new[]
            {
                Path.Combine(AppContext.BaseDirectory, "dcs-hook", "SimPad_Export.lua"),
                Path.Combine(AppContext.BaseDirectory, "..", "..", "..", "..", "..", "dcs-hook", "SimPad_Export.lua"),
                Path.Combine(Directory.GetCurrentDirectory(), "dcs-hook", "SimPad_Export.lua"),
                Path.Combine(Directory.GetCurrentDirectory(), "..", "dcs-hook", "SimPad_Export.lua")
            };

            foreach (var path in searchPaths)
            {
                if (File.Exists(path))
                {
                    return Results.Text(File.ReadAllText(path), "text/plain");
                }
            }

            return Results.NotFound(new { Error = "Hook script file not found on server" });
        })
        .WithName("GetDcsHookScript");

        group.MapPost("/dcs/install-hook", () =>
        {
            var userProfile = Environment.GetFolderPath(Environment.SpecialFolder.UserProfile);
            var possibleDcsDirs = new[]
            {
                Path.Combine(userProfile, "Saved Games", "DCS"),
                Path.Combine(userProfile, "Saved Games", "DCS.openbeta")
            };

            var installedPaths = new List<string>();
            var scriptContent = "";

            var searchPaths = new[]
            {
                Path.Combine(AppContext.BaseDirectory, "dcs-hook", "SimPad_Export.lua"),
                Path.Combine(AppContext.BaseDirectory, "..", "..", "..", "..", "..", "dcs-hook", "SimPad_Export.lua"),
                Path.Combine(Directory.GetCurrentDirectory(), "dcs-hook", "SimPad_Export.lua"),
                Path.Combine(Directory.GetCurrentDirectory(), "..", "dcs-hook", "SimPad_Export.lua")
            };

            foreach (var path in searchPaths)
            {
                if (File.Exists(path))
                {
                    scriptContent = File.ReadAllText(path);
                    break;
                }
            }

            if (string.IsNullOrEmpty(scriptContent))
            {
                return Results.BadRequest(new { Success = false, Message = "Could not locate source SimPad_Export.lua" });
            }

            foreach (var dcsDir in possibleDcsDirs)
            {
                if (Directory.Exists(dcsDir))
                {
                    var hooksDir = Path.Combine(dcsDir, "Scripts", "Hooks");
                    Directory.CreateDirectory(hooksDir);
                    var targetFile = Path.Combine(hooksDir, "SimPad_Hook.lua");
                    File.WriteAllText(targetFile, scriptContent);
                    installedPaths.Add(targetFile);
                }
            }

            return Results.Ok(new
            {
                Success = installedPaths.Count > 0,
                Message = installedPaths.Count > 0 ? "DCS Hook script successfully installed!" : "No DCS Saved Games directories found.",
                InstalledLocations = installedPaths
            });
        })
        .WithName("AutoInstallDcsHook");

        group.MapGet("/bms/status", (ActiveSimManager simManager) =>
        {
            return Results.Ok(new
            {
                Simulator = "FalconBMS",
                IsConnected = simManager.FalconProvider.IsConnected,
                SharedMemoryAvailable = simManager.FalconProvider.IsConnected
            });
        })
        .WithName("GetBmsStatus");

        return routes;
    }
}
