package com.serbekun.ss.http.handles.v0;

import com.serbekun.ss.service.youtube.YoutubeService;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

public class ApiV0YoutubeHttp {

    private final YoutubeService youtubeService;

    public ApiV0YoutubeHttp(YoutubeService youtubeService) {
        this.youtubeService = youtubeService;
    }

    public void handleInfo(Context ctx) {
        String url = ctx.queryParam("url");
        if (url == null || url.isBlank()) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.result("{\"error\": \"url parameter is required\"}");
            return;
        }

        try {
            String info = youtubeService.getVideoInfo(url);
            ctx.contentType("application/json");
            ctx.result(info);
        } catch (IllegalStateException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.result("{\"error\": \"" + e.getMessage().replace("\"", "'") + "\"}");
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.result("{\"error\": \"Failed to fetch video info\"}");
        }
    }

    public void handleDownload(Context ctx) {
        String url = ctx.queryParam("url");
        if (url == null || url.isBlank()) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.result("{\"error\": \"url parameter is required\"}");
            return;
        }

        try {
            byte[] videoBytes = youtubeService.downloadVideo(url);
            ctx.contentType("video/mp4");
            ctx.result(videoBytes);
        } catch (IllegalStateException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.result("{\"error\": \"" + e.getMessage().replace("\"", "'") + "\"}");
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.result("{\"error\": \"Failed to download video\"}");
        }
    }
}
