package com.serbekun.ss.repository;

import com.serbekun.ss.domain.models.LocalTokens;
import com.serbekun.ss.service.autosave.interfaces.AutoSavable;

public interface LocalTokensRepository extends AutoSavable {
    LocalTokens getTokens();
}