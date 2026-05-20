package com.serbekun.ss.http.handles.v0.dto.links;

public record V0LinksPutRequest(
    String uuid,
    String token,
    String url,
    String name,
    String description
) {}
