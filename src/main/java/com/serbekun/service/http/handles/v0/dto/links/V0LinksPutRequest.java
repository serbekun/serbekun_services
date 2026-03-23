package com.serbekun.service.http.handles.v0.dto.links;

public class V0LinksPutRequest {
    public String uuid;
    public String token;
    public String url;
    public String name;
    public String description;

    public V0LinksPutRequest(String uuid,
        String token, 
        String url,
        String name,
        String description 
    ) {
        
        this.uuid = uuid;
        this.url = url;
        this.name = name;
        this.description = description;
    }
}