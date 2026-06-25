package com.serbekun.ss.http.handles.v0;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import com.serbekun.ss.service.youtube.YoutubeService;

public class ApiV0YoutubeHttp {

    private final YoutubeService youtubeService;

    public ApiV0YoutubeHttp(YoutubeService youtubeService) {
        this.youtubeService = youtubeService;
    }

    public void handleInfo(Context ctx) {
        String url = ctx.queryParam("url");
        if (url == null || url.isBlank()) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.result("{\"error\": \"url parameter is required.\"}");
            return;
        }

        try {
            String info = youtubeService.getVideoInfo(url);
            ctx.contentType("application/json");
            ctx.result(info);
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.result("{\"error\": \"" + e.getMessage().replace("\"", "'") + "\"}");
        } catch (IllegalStateException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.result("{\"error\": \"" + e.getMessage().replace("\"", "'") + "\"}");
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.result("{\"error\": \"" + e.getClass().getSimpleName() + ": " + e.getMessage().replace("\"", "'") + "\"}");
        }
    }

    public void handleDownload(Context ctx) {
        String url = ctx.queryParam("url");
        if (url == null || url.isBlank()) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.result("{\"error\": \"url parameter is required.\"}");
            return;
        }

        try {
            byte[] videoBytes = youtubeService.downloadVideo(url);
            ctx.contentType("video/mp4");
            ctx.result(videoBytes);
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.result("{\"error\": \"" + e.getMessage().replace("\"", "'") + "\"}");
        } catch (IllegalStateException e) {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.result("{\"error\": \"" + e.getMessage().replace("\"", "'") + "\"}");
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            ctx.result("{\"error\": \"" + e.getClass().getSimpleName() + ": " + e.getMessage().replace("\"", "'") + "\"}");
        }
    }
}
