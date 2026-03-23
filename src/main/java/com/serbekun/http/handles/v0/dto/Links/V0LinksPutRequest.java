package com.serbekun.http.handles.v0.dto.Links;

public class V0LinksPutRequest {
    public String uuid;
    public String token;
    public String url;
    public String name;
    public String description;

    public V0LinksPutRequest() {
    }

    public V0LinksPutRequest(String uuid,
        String token,
        String url,
        String name,
        String description
    ) {
        this.uuid = uuid;
        this.token = token;
        this.url = url;
        this.name = name;
        this.description = description;
    }
}
