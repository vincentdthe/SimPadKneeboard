using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.Routing;
using SimPad.Kneeboard.Server.Models;
using SimPad.Kneeboard.Server.Services;

namespace SimPad.Kneeboard.Server.Endpoints;

public static class KneeboardEndpoints
{
    public static IEndpointRouteBuilder MapKneeboardEndpoints(this IEndpointRouteBuilder routes)
    {
        var group = routes.MapGroup("/api/kneeboard");

        group.MapGet("/tabs", (IKneeboardHierarchyService hierarchyService, string? profileId) =>
        {
            var tabs = hierarchyService.ResolveTabs(profileId);
            return Results.Ok(tabs);
        })
        .WithName("GetKneeboardTabs")
        .WithSummary("Get all resolved kneeboard tabs according to fallback hierarchy for current active sim/module.");

        group.MapGet("/tabs/{tabId}", (IKneeboardHierarchyService hierarchyService, string tabId, string? profileId) =>
        {
            var tab = hierarchyService.GetTabById(tabId, profileId);
            return tab != null ? Results.Ok(tab) : Results.NotFound();
        })
        .WithName("GetKneeboardTabById");

        group.MapGet("/pages/content", (IDocumentService documentService, string path, int? page) =>
        {
            if (string.IsNullOrEmpty(path) || !File.Exists(path))
            {
                return Results.NotFound(new { Error = "File not found" });
            }

            var contentType = documentService.GetContentType(path);
            var stream = documentService.OpenRead(path);
            if (stream == null)
            {
                return Results.NotFound();
            }

            return Results.File(stream, contentType, enableRangeProcessing: true);
        })
        .WithName("GetPageContent")
        .WithSummary("Stream raw PDF or Image page contents with HTTP range support.");

        group.MapGet("/pages/info", (IDocumentService documentService, string path) =>
        {
            if (string.IsNullOrEmpty(path) || !File.Exists(path))
            {
                return Results.NotFound(new { Error = "File not found" });
            }

            var fileInfo = new FileInfo(path);
            var fileType = documentService.GetFileType(path);
            int pageCount = fileType == PageFileType.PDF ? documentService.GetPdfPageCount(path) : 1;

            return Results.Ok(new
            {
                FileName = fileInfo.Name,
                FilePath = path,
                FileType = fileType.ToString(),
                FileSizeBytes = fileInfo.Length,
                PageCount = pageCount,
                LastModifiedUtc = fileInfo.LastWriteTimeUtc
            });
        })
        .WithName("GetPageInfo");

        return routes;
    }
}
