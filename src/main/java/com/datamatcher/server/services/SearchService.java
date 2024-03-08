package com.datamatcher.server.services;

import com.datamatcher.server.repositories.DropBoxRepo;
import com.datamatcher.server.repositories.SearchRepo;
import com.datamatcher.server.utils.JSON;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

@Service
public class SearchService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final SearchRepo repo;
    private final DropBoxRepo dropBoxRepo;

    @Autowired
    public SearchService(final SearchRepo repo, final DropBoxRepo dropBoxRepo){
        this.repo = repo;
        this.dropBoxRepo = dropBoxRepo;
    }
    public final List<Map<String, List<String>>> search(final String recordType,
                                                        final String filter,
                                                        final SearchRepo.FilterType filterType,
                                                        final SearchRepo.EnrichmentMethod enrichmentMethod,
                                                        final List<String> joinOn,
                                                        final int maxDepth,
                                                        final int skip,
                                                        final int limit){
        return repo.search(recordType, filter, filterType, enrichmentMethod, joinOn, maxDepth, skip, limit);
    }

    private final void updateExportStatus(final String exportId, final String status){
        logger.info("Export '{}' status: {}.", exportId, status);
    }

    public final Map<String, Object> export(final String recordType,
                                            final String filter,
                                            final SearchRepo.FilterType filterType,
                                            final SearchRepo.EnrichmentMethod enrichmentMethod,
                                            final List<String> joinOn,
                                            final int maxDepth,
                                            final String path){
        final String id = UUID.randomUUID().toString();
        updateExportStatus(id, "export_started");
        Thread.startVirtualThread(()->{
            final String[] header = repo.getMappings().toArray(new String[]{});
            final Map<String, Integer> headerMap = new HashMap<>(header.length * 2);
            for (int i = 0; i < header.length; i++) {
                headerMap.put(header[i], i);
            }
            final CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                    .setHeader(header)
                    .build();
            final File file;
            try {
                file = File.createTempFile("export", ".csv");
                file.deleteOnExit();
            }catch (final Throwable cause){
                logger.error("Failed to create tmp file", cause);
                updateExportStatus(id, "failed");
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create tmp file", cause);
            }
            updateExportStatus(id, "temp_file_created");
            final FileWriter fw;
            try {
                fw = new FileWriter(file);
            }catch (final Throwable cause){
                logger.error("Failed to create file writer", cause);
                updateExportStatus(id, "failed");
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create file writer", cause);
            }
            updateExportStatus(id, "file_writer_created");
            try (final CSVPrinter printer = new CSVPrinter(fw, csvFormat)) {
                updateExportStatus(id, "searching");
                int skip = 0;
                final int limit = 100;
                for(;;){
                    final List<Map<String, List<String>>> searchResult = search(recordType, filter, filterType, enrichmentMethod, joinOn, maxDepth, skip, limit);
                    if (searchResult.isEmpty())
                        break;

                    skip += limit;

                    for (final Map<String, List<String>> record : searchResult) {
                        final List<String> sortedRecord = new ArrayList<>(headerMap.size());
                        for (int i = 0; i < headerMap.size(); i++) {
                            sortedRecord.add("");
                        }
                        record.remove("_id");
                        for(final Map.Entry<String, List<String>> e : record.entrySet()){
                            final List<String> val = e.getValue();
                            if (val.size() == 0){
                                sortedRecord.set(headerMap.get(e.getKey()), "");
                            }else if (val.size() == 1){
                                sortedRecord.set(headerMap.get(e.getKey()), val.get(0));
                            }else{
                                sortedRecord.set(headerMap.get(e.getKey()), JSON.toJson(e.getValue()));
                            }
                        }
                        printer.printRecord(sortedRecord);
                    }
                    updateExportStatus(id, "searching_" + skip + "_" + limit);
                }
            } catch (final Throwable cause) {
                logger.error("Failed to write csv", cause);
                updateExportStatus(id, "failed");
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to write csv", cause);
            } finally {
                try {
                    fw.close();
                }catch (final Throwable cause){
                    logger.error("Failed to close file writer", cause);
                }
            }
            updateExportStatus(id, "uploading to docker");
            final DropBoxRepo.DropBoxEntity entity = dropBoxRepo.upload(path, file);
            updateExportStatus(id, "finished");
            logger.info("Export finished: {}.", JSON.toJson(entity));
        });

        return Map.of("success", true, "id", id);
    }
}
