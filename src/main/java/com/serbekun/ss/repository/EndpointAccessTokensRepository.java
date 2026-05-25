package com.serbekun.ss.repository;

import com.serbekun.ss.domain.models.EndpointsAccessTokens;
import com.serbekun.ss.service.autosave.interfaces.AutoSavable;

public interface EndpointAccessTokensRepository extends AutoSavable {
    EndpointsAccessTokens getEndpointAccessTokens();
}