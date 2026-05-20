package com.serbekun.ss.service.http.handles.v0.dto.links;

public class V0LinksDeleteRequest {
    public String uuid;
    public String token;

    public V0LinksDeleteRequest(String uuid, String token) {
        this.uuid = uuid;
        this.token = token;
    }
}
