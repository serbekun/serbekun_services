/** Response DTO returned after a link repository has been created. */
package com.serbekun.ss.domain.dto.http.linksrepo;

import java.util.UUID;

public record V0RepositoryPostResponse(UUID repositoryId, String token, String name, String createdAt) {}
