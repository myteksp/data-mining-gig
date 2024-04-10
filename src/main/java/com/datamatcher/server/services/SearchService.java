package com.datamatcher.server.services;

import com.datamatcher.server.entities.DataType;
import com.datamatcher.server.repositories.DropBoxRepo;
import com.datamatcher.server.repositories.SearchRepo;
import com.datamatcher.server.utils.JSON;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.*;
import java.nio.file.Files;
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

    public final List<String> getMappings(){
        return repo.getMappings();
    }

    public final ResponseEntity<Resource> matchFile(final MultipartFile multipartFile,
                                                    final String srcColumn,
                                                    final String dstColumn,
                                                    final SearchRepo.EnrichmentMethod enrichmentMethod,
                                                    final String joinOn,
                                                    final int maxDepth,
                                                    final DataType src_type,
                                                    final boolean withHeader) {
        if (src_type == DataType.JSON){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "JSON is not supported in matching.");
        }
        final File file = multipartToTemp(multipartFile);
        final DataType type = detectType(src_type, file);
        final CSVParser csvParser;
        try {
            csvParser = new CSVParser(new BufferedReader(new FileReader(file)), convertFormat(type, withHeader));
        } catch (final Throwable cause) {
            logger.error("Failed to open file '{}'.", multipartFile.getOriginalFilename(), cause);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to parse file.");
        }
        final String srcColumnName = getSourceColumn(srcColumn);
        final File resultFile;
        try {
            resultFile = File.createTempFile("result_", "csv");
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create temp file", e);
        }
        resultFile.deleteOnExit();
        final FileWriter fw;
        try {
            fw = new FileWriter(resultFile);
        }catch (final Throwable cause){
            logger.error("Failed to create file writer", cause);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create file writer", cause);
        }
        final String[] header = repo.getMappings().toArray(new String[]{});
        final Map<String, Integer> headerMap = new HashMap<>(header.length * 2);
        for (int i = 0; i < header.length; i++) {
            headerMap.put(header[i], i);
        }
        final CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(header)
                .build();
        int recordCounter = 0;
        try (final CSVPrinter printer = new CSVPrinter(fw, csvFormat)) {
            for (final CSVRecord record : csvParser) {
                recordCounter++;
                final String value = transformSourceColumn(record.get(srcColumnName), srcColumn);
                logger.info("Processing record number: {}. Extracted value: '{}'.", recordCounter, value);
                final List<Map<String, List<String>>> res = search(dstColumn, value, SearchRepo.FilterType.EQUALS, enrichmentMethod, List.of(joinOn), maxDepth, 0, 1);
                if (!res.isEmpty()){
                    final Map<String, List<String>> result = res.getFirst();
                    final List<String> sortedRecord = new ArrayList<>(headerMap.size());
                    for (int i = 0; i < headerMap.size(); i++) {
                        sortedRecord.add("");
                    }
                    result.remove("_id");
                    for(final Map.Entry<String, List<String>> e : result.entrySet()){
                        final List<String> val = e.getValue();
                        if (val.isEmpty()){
                            sortedRecord.set(headerMap.get(e.getKey()), "");
                        }else if (val.size() == 1){
                            sortedRecord.set(headerMap.get(e.getKey()), val.getFirst());
                        }else{
                            sortedRecord.set(headerMap.get(e.getKey()), JSON.toJson(e.getValue()));
                        }
                    }
                    printer.printRecord(sortedRecord);
                }
            }
        }catch (final Throwable cause){
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to write csv", cause);
        } finally {
            try {
                fw.close();
            }catch (final Throwable cause){
                logger.error("Failed to close file writer", cause);
            }
        }

        try {
            return ResponseEntity.ok()
                    .contentLength(file.length())
                    .header("Content-Type", "text/csv", "charset=utf-8")
                    .body(new InputStreamResource(new FileInputStream(resultFile)));
        } catch (FileNotFoundException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to open result csv", e);
        }
    }
    //======================
    private final String transformSourceColumn(final String value, final String src){
        return value;
    }
    private final String getSourceColumn(final String src){
        return src;
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
                            if (val.isEmpty()){
                                sortedRecord.set(headerMap.get(e.getKey()), "");
                            }else if (val.size() == 1){
                                sortedRecord.set(headerMap.get(e.getKey()), val.getFirst());
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

    private final File multipartToTemp(final MultipartFile file){
        final File tempFile;
        try {
            tempFile = File.createTempFile("matching_", "tmp");
        }catch (final Throwable cause){
            logger.error("Failed to create tmp file.", cause);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create tmp file", cause);
        }
        tempFile.deleteOnExit();
        try {
            file.transferTo(tempFile);
        }catch (final Throwable cause){
            logger.error("Failed to save to tmp file.", cause);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save to tmp file", cause);
        }
        return tempFile;
    }

    private final DataType autodetect(final File file){
        for (final DataType type : DataType.values()){
            if (type != DataType.AUTODETECT){
                final CSVParser csvParser;
                try {
                    csvParser = new CSVParser(new BufferedReader(new FileReader(file)), convertFormat(type, true));
                    final Iterator<CSVRecord> iterator = csvParser.iterator();
                    int counter = 0;
                    while (iterator.hasNext()) {
                        final CSVRecord record = iterator.next();
                        counter++;
                        if (counter > 5)
                            break;

                        final String[] values = record.values();
                        logger.info("Autodetect values '{}'", JSON.toJson(values));
                    }
                    return type;
                } catch (final Throwable ignored) {}
            }
        }

        try {
            final List<?> data = JSON.fromJson(Files.readString(file.toPath()), List.class);
            logger.info("Detected json: {}", JSON.toJson(data));
            return DataType.JSON;
        } catch (final Throwable cause) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to autodetect file type.");
        }
    }
    private final DataType detectType(final DataType src_type, final File file){
        if (src_type == DataType.AUTODETECT){
            return autodetect(file);
        }else{
            return src_type;
        }
    }

    private static final CSVFormat convertFormat(final DataType dataType, final boolean withHeader){
        final CSVFormat result = switch (dataType){
            case JSON -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Illegal data type: '" + dataType + "'.");
            case DEFAULT_CSV -> CSVFormat.DEFAULT;
            case MONGODB_CSV -> CSVFormat.POSTGRESQL_CSV;
            case MONGODB_TSV -> CSVFormat.MONGODB_TSV;
            case EXCEL -> CSVFormat.EXCEL;
            case INFORMIX_UNLOAD -> CSVFormat.INFORMIX_UNLOAD;
            case INFORMIX_UNLOAD_CSV -> CSVFormat.INFORMIX_UNLOAD_CSV;
            case TDF -> CSVFormat.TDF;
            case MYSQL -> CSVFormat.MYSQL;
            case ORACLE -> CSVFormat.ORACLE;
            case POSTGRESQL_CSV -> CSVFormat.POSTGRESQL_CSV;
            case POSTGRESQL_TEXT -> CSVFormat.POSTGRESQL_TEXT;
            case RFC4180 -> CSVFormat.RFC4180;
            case AUTODETECT -> throw new ResponseStatusException(HttpStatus.ALREADY_REPORTED, "This file already uploaded.");
        };
        if (withHeader){
            return result.withHeader();
        }else{
            return result;
        }
    }
}
