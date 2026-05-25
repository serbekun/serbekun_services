package com.serbekun.ss.repository;

import com.serbekun.ss.domain.models.Links;
import com.serbekun.ss.service.autosave.interfaces.AutoSavable;

public interface LinksRepository extends AutoSavable {
    Links getLinks();
}