using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Routing;
using SimPad.Kneeboard.Server.Models;
using SimPad.Kneeboard.Server.Services;

namespace SimPad.Kneeboard.Server.Endpoints;

public static class ProfileEndpoints
{
    public static IEndpointRouteBuilder MapProfileEndpoints(this IEndpointRouteBuilder routes)
    {
        var group = routes.MapGroup("/api/profiles");

        group.MapGet("/", (IProfileService profileService) =>
        {
            var list = profileService.GetAllProfiles();
            return Results.Ok(list);
        })
        .WithName("GetAllProfiles");

        group.MapGet("/active", (IProfileService profileService) =>
        {
            var active = profileService.GetActiveProfile();
            return Results.Ok(active);
        })
        .WithName("GetActiveProfile");

        group.MapGet("/{id}", (IProfileService profileService, string id) =>
        {
            var profile = profileService.GetProfileById(id);
            return profile != null ? Results.Ok(profile) : Results.NotFound();
        })
        .WithName("GetProfileById");

        group.MapPost("/", (IProfileService profileService, Profile profile) =>
        {
            var saved = profileService.CreateOrUpdateProfile(profile);
            return Results.Ok(saved);
        })
        .WithName("SaveProfile");

        group.MapPost("/active/{id}", (IProfileService profileService, string id) =>
        {
            var success = profileService.SetActiveProfile(id);
            return success ? Results.Ok(new { Success = true, ActiveProfileId = id }) : Results.NotFound();
        })
        .WithName("SetActiveProfile");

        group.MapDelete("/{id}", (IProfileService profileService, string id) =>
        {
            var success = profileService.DeleteProfile(id);
            return success ? Results.Ok(new { Success = true }) : Results.NotFound();
        })
        .WithName("DeleteProfile");

        return routes;
    }
}
