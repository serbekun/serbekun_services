package com.serbekun.ss.http.handles.api;

import com.serbekun.ss.domain.dto.http.VersionResponse;

import io.javalin.http.Context;

public class ApiVersion {

    public void main(Context ctx) {

        VersionResponse versionResponse = new VersionResponse("alfa-2026-06-26");
        ctx.json(versionResponse);
    }
}
