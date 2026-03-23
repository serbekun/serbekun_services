package com.serbekun.service.http.handles.v0.dto.links;

public class V0LinksPostRequest {
    public String url;
    public String name;
    public String description;
    
    public V0LinksPostRequest(String url, String name, String description) {
        this.url = url;
        this.name = name;
        this.description = description;
    }
}
