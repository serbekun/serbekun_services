/** Request DTO for creating a short url. */
package com.serbekun.ss.domain.dto.http.shorturl;

public record V0ShortUrlPostRequest(String url, String name, String description) {}
