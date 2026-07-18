package com.serbekun.ss.http.handles.statics;

import java.util.function.Function;

import io.javalin.http.ContentType;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import com.serbekun.ss.service.resource.ResourcesService;

public class StaticV0Http {

    private final ResourcesService resourcesService;

    public StaticV0Http(ResourcesService resourcesService) {
        this.resourcesService = resourcesService;
    }

    public void main(Context ctx, StaticResource resource) {
        main(ctx, ctx.pathParam("name"), resource);
    }

    public void main(Context ctx, String name, StaticResource resource) {
        if (name == null) {
            name = "";
        }

        if (name.isEmpty()) {
            ctx.contentType(resource.listContentType);
            String files = resource.list(resourcesService);
            if (files == null) {
                ctx.status(HttpStatus.NOT_FOUND);
                return;
            }

            ctx.result(files);
            return;
        }

        ctx.contentType(resource.contentType(resourcesService, name));

        if (resource.binary) {
            byte[] data = resource.binaryReader.apply(resourcesService, name);
            if (data == null) {
                ctx.status(HttpStatus.NOT_FOUND);
                return;
            }

            ctx.result(data);
            return;
        }

        String data = resource.textReader.read(resourcesService, name);
        if (data == null) {
            ctx.status(HttpStatus.NOT_FOUND);
            return;
        }

        ctx.result(data);
    }

    public enum StaticResource {
        CSS(false, ContentType.CSS, ContentType.JSON, ResourcesService::getCss, null, null),
        DOMAIN(false, "text/plain; charset=utf-8", "application/json", ResourcesService::getDomain, null, ResourcesService::listDomainsAsJson),
        HTML(false, "text/html", "application/json", ResourcesService::getHtml, null, null),
        IMAGES(true, null, ContentType.IMAGE_JPEG.getMimeType(), null, ResourcesService::getImage, ResourcesService::listImagesAsJson),
        JS(false, ContentType.JAVASCRIPT, ContentType.JSON, ResourcesService::getJs, null, null),
        JSON(false, "application/json", "application/json", ResourcesService::getJson, null, null),
        PDF(true, null, "application/json", null, ResourcesService::getPdf, ResourcesService::listPdfsAsJson),
        SVG(false, ContentType.IMAGE_SVG.getMimeType(), ContentType.JSON, ResourcesService::getSvg, null, null);

        private final boolean binary;
        private final String contentType;
        private final String listContentType;
        private final ResourceTextReader textReader;
        private final ResourceBinaryReader binaryReader;
        private final Function<ResourcesService, String> listReader;

        StaticResource(boolean binary,
                       String contentType,
                       String listContentType,
                       ResourceTextReader textReader,
                       ResourceBinaryReader binaryReader,
                       Function<ResourcesService, String> listReader) {
            this.binary = binary;
            this.contentType = contentType;
            this.listContentType = listContentType;
            this.textReader = textReader;
            this.binaryReader = binaryReader;
            this.listReader = listReader;
        }

        private String contentType(ResourcesService resourcesService, String name) {
            if (contentType != null) {
                return contentType;
            }
            return resourcesService.detectMimeType(name);
        }

        private String list(ResourcesService resourcesService) {
            if (listReader != null) {
                return listReader.apply(resourcesService);
            }
            return textReader.read(resourcesService, "");
        }
    }

    @FunctionalInterface
    private interface ResourceTextReader {
        String read(ResourcesService resourcesService, String name);
    }

    @FunctionalInterface
    private interface ResourceBinaryReader {
        byte[] apply(ResourcesService resourcesService, String name);
    }
}
