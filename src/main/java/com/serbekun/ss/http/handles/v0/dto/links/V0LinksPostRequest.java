/** Record for creating a new link via POST request. */
package com.serbekun.ss.http.handles.v0.dto.links;

public record V0LinksPostRequest(String url, String name, String description) {}
