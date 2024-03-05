package com.datamatcher.server.controllers;

import com.datamatcher.server.entities.DataType;
import com.datamatcher.server.entities.UploadResponse;
import com.datamatcher.server.repositories.RecordsRepo;
import com.datamatcher.server.services.UploadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/uploads")
public class UploadsController {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final UploadService uploadService;

    @Autowired
    public UploadsController(final UploadService uploadService){
        this.uploadService = uploadService;
    }

    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public final List<Map<String, List<String>>> search(@RequestParam(value = "recordType", defaultValue = "") final String recordType,
                                                        @RequestParam(value = "filter", defaultValue = "") final String filter,
                                                        @RequestParam(value = "filterType", defaultValue = "NONE") final RecordsRepo.FilterType filterType,
                                                        @RequestParam(value = "enrichmentMethod", defaultValue = "NONE") final RecordsRepo.EnrichmentMethod enrichmentMethod,
                                                        @RequestParam(value = "joinOn", required = true) final List<String> joinOn,
                                                        @RequestParam(value = "maxDepth", defaultValue = "10") final int maxDepth,
                                                        @RequestParam(value = "skip", defaultValue = "0") final int skip,
                                                        @RequestParam(value = "limit", defaultValue = "100") final int limit){
        return uploadService.search(recordType, filter, filterType, enrichmentMethod, joinOn, maxDepth, skip, limit);
    }

    @GetMapping(value = "/listUnfinishedUploads", produces = MediaType.APPLICATION_JSON_VALUE)
    public final List<UploadResponse> listUnfinishedUploads(){
        return uploadService.listUnfinishedUploads();
    }


    @GetMapping(value = "/getMappings", produces = MediaType.APPLICATION_JSON_VALUE)
    public final List<String> getMappings(){
        return uploadService.getMappings();
    }

    @PostMapping(value = "/upload", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public final UploadResponse upload(@RequestParam("file") final MultipartFile file,
                                       @RequestParam("mappings") final List<String> mappings,
                                       @RequestParam(value = "type", defaultValue = "DEFAULT_CSV") final DataType type,
                                       @RequestParam(value = "withHeader", defaultValue = "true") final boolean withHeader){
        final UploadResponse response = uploadService.upload(type, mappings, file, withHeader);
        logger.info("Uploaded: '{}'.", response);
        return response;
    }

    @PostMapping(value = "/ingestFromDropBox", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.OK)
    public final UploadResponse ingestFromDropBox(@RequestPart("path") final String path,
                                                  @RequestParam("mappings") final List<String> mappings,
                                                  @RequestParam(value = "type", defaultValue = "DEFAULT_CSV") final DataType type,
                                                  @RequestParam(value = "withHeader", defaultValue = "true") final boolean withHeader){
        final UploadResponse response = uploadService.ingestFromDropBox(type, mappings, path, withHeader);
        logger.info("Uploaded: '{}'.", response);
        return response;
    }

    @GetMapping(value = "/uploadStatus", produces = MediaType.APPLICATION_JSON_VALUE)
    public final UploadResponse getStatus(@RequestParam(value = "uploadId") final String id){
        return uploadService.getUploadById(id);
    }
}
