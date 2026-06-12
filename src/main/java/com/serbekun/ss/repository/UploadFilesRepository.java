package com.serbekun.ss.repository;

import com.serbekun.ss.domain.models.UploadedFiles;
import com.serbekun.ss.service.autosave.interfaces.AutoSavable;

public interface UploadFilesRepository extends AutoSavable {
    UploadedFiles UploadedFiles();
}
