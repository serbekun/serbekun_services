package com.serbekun.ss.service.youtube;

import java.io.IOException;

public class YoutubeService {

    public byte[] downloadVideo(String url) throws IOException {
        return Youtube.DownloadVideoByUrl(url);
    }

    public String getVideoInfo(String url) throws IOException {
        return Youtube.GetVideoInfo(url);
    }
}
