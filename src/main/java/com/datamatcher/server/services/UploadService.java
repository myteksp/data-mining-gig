package com.datamatcher.server.services;

import com.datamatcher.server.entities.DataMapping;
import com.datamatcher.server.entities.DataRecord;
import com.datamatcher.server.entities.DataType;
import com.datamatcher.server.entities.UploadResponse;
import com.datamatcher.server.repositories.DropBoxRepo;
import com.datamatcher.server.repositories.RecordsRepo;
import com.datamatcher.server.utils.JSON;
import com.datamatcher.server.utils.Signature;
import com.datamatcher.server.utils.StringTransformer;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.util.*;

@Service
public final class UploadService {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final RecordsRepo repo;
    private final DropBoxRepo dropBoxRepo;
    @Autowired
    public UploadService(final RecordsRepo repo,
                         final DropBoxRepo dropBoxRepo){
        this.repo = repo;
        this.dropBoxRepo = dropBoxRepo;
    }


    public final List<String> getMappings(){
        return repo.getMappings();
    }

    public final UploadResponse ingestFromDropBox(final DataType type_input,
                                                  final List<String> mappings,
                                                  final String path,
                                                  final boolean withHeaders){
        if (!dropBoxRepo.doesFileExists(path)){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File '" + path + "' is not found in dropbox.");
        }
        final UploadResponse response = new UploadResponse(path, generateUploadId(type_input, mappings, path, withHeaders), DataMapping.parseMappings(mappings), type_input, false, 0, 0);
        if (repo.getUploadById(response.uploadId) != null){
            throw new ResponseStatusException(HttpStatus.ALREADY_REPORTED, "This file already uploaded.");
        }
        response.outOf = dropBoxRepo.getFileSize(path);
        final Thread t = new Thread(()->{
            final UploadResponse r = _upload(type_input, dropBoxRepo.getDropboxFile(path), withHeaders, response);
            logger.info("File copied from dropbox. {}", JSON.toJson(r));
        }, "DB_DOWNLOAD_" + path);
        t.setPriority(Thread.MAX_PRIORITY);
        t.start();
        return response;
    }
    private final UploadResponse _upload(final DataType type_input,
                                         final File tempFile,
                                         final boolean withHeaders,
                                         final UploadResponse initial_response){
        UploadResponse response = initial_response;
        response.outOf = tempFile.length();
        response = repo.createUpload(response);
        repo.ensureIndexes(response.mappings);
        repo.saveMappings(response.mappings);
        if (type_input == DataType.AUTODETECT){
            response.dataType = autodetect(tempFile);
        }
        final DataType type = response.dataType;

        final UploadResponse finalResponse = response;
        Thread.startVirtualThread(()->{
            try {
                if (type == DataType.JSON) {
                    uploadJson(finalResponse.uploadId, finalResponse.mappings, tempFile);
                } else {
                    uploadCsv(finalResponse.fileName, finalResponse.uploadId, finalResponse.mappings, tempFile, type, withHeaders);
                }
            }finally {
                tempFile.delete();
            }
        });
        return response;
    }
    public final UploadResponse upload(final DataType type_input,
                                       final List<String> mappings,
                                       final MultipartFile file,
                                       final boolean withHeaders){
        UploadResponse response = new UploadResponse(file.getOriginalFilename(), generateUploadId(type_input, mappings, file, withHeaders), DataMapping.parseMappings(mappings), type_input, false, 0, 0);
        if (repo.getUploadById(response.uploadId) != null){
            throw new ResponseStatusException(HttpStatus.ALREADY_REPORTED, "This file already uploaded.");
        }
        final File tempFile;
        try {
            tempFile = File.createTempFile("up_" + response.uploadId, "tmp");
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
        return _upload(type_input, tempFile, withHeaders, response);
    }

    public final UploadResponse getUploadById(final String id){
        final UploadResponse response = repo.getUploadById(id);
        if (response == null){
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Upload '" + id + "' not found.");
        }
        return response;
    }


    public final List<Map<String, List<String>>> search(final String recordType,
                                                        final String filter,
                                                        final RecordsRepo.FilterType filterType,
                                                        final RecordsRepo.EnrichmentMethod enrichmentMethod,
                                                        final List<String> joinOn,
                                                        final int maxDepth,
                                                        final int skip,
                                                        final int limit){
        return repo.search(recordType, filter, filterType, enrichmentMethod, joinOn, maxDepth, skip, limit);
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
    private final void uploadJson(final String uploadId,
                                  final List<DataMapping> mappings,
                                  final File file) {
        final List<?> data;
        try {
            data = JSON.fromJson(Files.readString(file.toPath()), List.class);
        } catch (final Throwable cause) {
            logger.error("Failed to parse data: ", cause);
            repo.completeUploadProgressWithError(uploadId, "Failed to parse data. Cause: " + cause.getMessage());
            return;
        }

        for (final Object o : data) {
            final Map<?, ?> map = (Map<?, ?>) o;
            final List<DataRecord> records = new ArrayList<>(map.size());
            for (final DataMapping e : mappings) {
                final Object valueObj = MapPropertyExtractor.getField(e.path, map);
                if (valueObj != null) {
                    final String value = valueObj.toString();
                    if (hasContent(value)) {
                        records.add(new DataRecord(e.name, StringTransformer.transform(value, e.transformations)));
                    }
                }
            }
            repo.saveRecord(records);
        }

        repo.updateUploadProgress(uploadId, file.length());
        repo.completeUploadProgress(uploadId);
    }


    private final void uploadCsv(final String fileName,
                                 final String uploadId,
                                 final List<DataMapping> mappings,
                                 final File file,
                                 final DataType dataType,
                                 final boolean withHeader) {

        final CSVParser csvParser;
        try {
            csvParser = new CSVParser(new BufferedReader(new FileReader(file)), convertFormat(dataType, withHeader));
        } catch (final Throwable cause) {
            logger.error("Failed to open file '{}'.", fileName, cause);
            repo.completeUploadProgressWithError(uploadId, "Failed to open file. Cause: " + cause.getMessage());
            return;
        }
        long byteCounter = 0;

        if (withHeader) {
            for (final CSVRecord csvRecord : csvParser) {
                byteCounter += Arrays.stream(csvRecord.values()).mapToInt(String::length).sum();
                final List<DataRecord> records = new ArrayList<>(csvRecord.size());
                for (final DataMapping e : mappings) {
                    final String value;
                    try{
                        value = csvRecord.get(e.path);
                    }catch (final Throwable t){
                        continue;
                    }
                    if (hasContent(value)) {
                        records.add(new DataRecord(e.name, StringTransformer.transform(value, e.transformations)));
                    }
                }
                repo.saveRecord(records);
                repo.updateUploadProgress(uploadId, byteCounter);
            }
        } else {
            for (final CSVRecord csvRecord : csvParser) {
                byteCounter += Arrays.stream(csvRecord.values()).mapToInt(r -> r.length() + 2).sum();
                final List<DataRecord> records = new ArrayList<>(csvRecord.size());
                for (final DataMapping e : mappings) {
                    final String value;
                    try{
                        value = csvRecord.get(Integer.parseInt(e.path));
                    }catch (final Throwable t){
                        continue;
                    }
                    if (hasContent(value)) {
                        records.add(new DataRecord(e.name, StringTransformer.transform(value, e.transformations)));
                    }
                }
                repo.saveRecord(records);
                repo.updateUploadProgress(uploadId, byteCounter);
            }
        }
        repo.updateUploadProgress(uploadId, file.length());
        repo.completeUploadProgress(uploadId);
        try {csvParser.close();}catch (final Throwable ignored){}
        logger.info("Upload '{}' completed.", uploadId);
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

    private static final String generateUploadId(final DataType type,
                                                 final List<String> mappings,
                                                 final MultipartFile file,
                                                 final boolean withHeaders)  {
        final String contentType = file.getContentType();
        if (contentType == null){
            return Signature.getSignature(List.of(type, mappings, file.getName(), withHeaders, file.getSize()));
        }
        return Signature.getSignature(List.of(type, mappings, file.getName(), withHeaders, file.getSize(), contentType));
    }
    private static final String generateUploadId(final DataType type,
                                                 final List<String> mappings,
                                                 final String path,
                                                 final boolean withHeaders)  {
        return Signature.getSignature(List.of(type, mappings, path, withHeaders));
    }


    private static final boolean hasContent(final String str){
        if (str == null)
            return false;

        if (str.isBlank())
            return false;

        return true;
    }

    private Map<String, String> parseMapping(final List<String> mappings){
        if (mappings.isEmpty()){
            logger.error("Upload rejected. Mappings are empty.");
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mappings are empty");
        }
        final Map<String, String> res = new HashMap<>(mappings.size() * 2);
        for(final String mapping : mappings){
            final String[] split = mapping.split(":");
            if (split.length != 2){
                logger.error("Upload rejected. Invalid mapping: '{}'.", mapping);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid mapping: '" + mapping + "'.");
            }
            res.put(split[0], split[1]);
        }
        return res;
    }



    private final static class MapPropertyExtractor {
        public static final Object getField(final String key, final Map<?,?> message){
            final Object result = message.get(key);
            if (result == null){
                return getFieldFromPath(key.split("\\."), message);
            }
            return result;
        }

        private static final Object getFieldFromPath(final String[] path, final Map<?,?> message){
            if (path.length == 0)
                return null;

            if (path.length == 1)
                return message.get(path[0]);

            final String key = path[0];
            final Object domainObj = message.get(key);
            if (domainObj instanceof Map<?,?>){
                final Map<?,?> domain = (Map<?, ?>) domainObj;
                final String[] reminder = new String[path.length - 1];
                System.arraycopy(path, 1, reminder, 0, reminder.length);
                return getFieldFromPath(reminder, domain);
            }else{
                return null;
            }
        }
    }
}
