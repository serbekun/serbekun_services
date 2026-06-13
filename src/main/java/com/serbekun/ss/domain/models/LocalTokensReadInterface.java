package com.serbekun.ss.domain.models;

import java.util.Map;

/**
* Interface for provide to {@link com.serbekun.ss.repository.LocalTokensFileRepo}
* File Repository class only read access
*/
public interface LocalTokensReadInterface {
    /**
     * @return All tokens data.
     */
    Map<String, String> getTokensData();
}
