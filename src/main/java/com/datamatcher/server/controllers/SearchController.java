package com.datamatcher.server.controllers;

import com.datamatcher.server.repositories.RecordsRepo;
import com.datamatcher.server.repositories.SearchRepo;
import com.datamatcher.server.services.SearchService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/search")
public class SearchController {
    private final SearchService service;
    public SearchController(final SearchService service){
        this.service = service;
    }

    @GetMapping(value = "/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public final List<Map<String, List<String>>> search(@RequestParam(value = "recordType", defaultValue = "") final String recordType,
                                                        @RequestParam(value = "filter", defaultValue = "") final String filter,
                                                        @RequestParam(value = "filterType", defaultValue = "NONE") final SearchRepo.FilterType filterType,
                                                        @RequestParam(value = "enrichmentMethod", defaultValue = "NONE") final SearchRepo.EnrichmentMethod enrichmentMethod,
                                                        @RequestParam(value = "joinOn", required = true) final List<String> joinOn,
                                                        @RequestParam(value = "maxDepth", defaultValue = "10") final int maxDepth,
                                                        @RequestParam(value = "skip", defaultValue = "0") final int skip,
                                                        @RequestParam(value = "limit", defaultValue = "100") final int limit){
        return service.search(recordType, filter, filterType, enrichmentMethod, joinOn, maxDepth, skip, limit);
    }

    @GetMapping(value = "/getMappings", produces = MediaType.APPLICATION_JSON_VALUE)
    public final List<String> getMappings(){
        return service.getMappings();
    }

    @GetMapping(value = "/exportToDropbox", produces = MediaType.APPLICATION_JSON_VALUE)
    public final Map<String, Object> exportToDropbox(@RequestParam(value = "recordType", defaultValue = "") final String recordType,
                                                      @RequestParam(value = "filter", defaultValue = "") final String filter,
                                                      @RequestParam(value = "filterType", defaultValue = "NONE") final SearchRepo.FilterType filterType,
                                                      @RequestParam(value = "enrichmentMethod", defaultValue = "NONE") final SearchRepo.EnrichmentMethod enrichmentMethod,
                                                      @RequestParam(value = "joinOn", required = true) final List<String> joinOn,
                                                      @RequestParam(value = "maxDepth", defaultValue = "10") final int maxDepth,
                                                      @RequestParam(value = "path") final String path){
        return service.export(recordType, filter, filterType, enrichmentMethod, joinOn, maxDepth, path);
    }

}
