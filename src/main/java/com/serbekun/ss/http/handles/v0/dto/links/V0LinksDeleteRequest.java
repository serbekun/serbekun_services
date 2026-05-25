/** Record for deleting a link via DELETE request. */
package com.serbekun.ss.http.handles.v0.dto.links;

public record V0LinksDeleteRequest(String uuid, String token) {}
