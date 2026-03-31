package com.serbekun.http.handles.v0.dto.Links;

public record V0LinksPutRequest(
    String uuid,
    String token,
    String url,
    String name,
    String description
) {}
