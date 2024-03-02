package com.datamatcher.server.repositories;

import com.datamatcher.server.entities.DataMapping;
import com.datamatcher.server.entities.DataRecord;
import com.datamatcher.server.entities.DataType;
import com.datamatcher.server.entities.UploadResponse;
import com.datamatcher.server.utils.JSON;
import com.datamatcher.server.utils.Signature;
import jakarta.annotation.PreDestroy;
import org.neo4j.driver.*;
import org.neo4j.driver.Record;
import org.neo4j.driver.types.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.stream.Collectors;

@Service
public final class RecordsRepo {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Driver driver;
    private final String database;
    private final String userName;
    private final String password;
    private final String uri;
    public RecordsRepo(@Value("${neo4j.uri}") final String uri,
                       @Value("${neo4j.user}") final String userName,
                       @Value("${neo4j.password}") final String password,
                       @Value("${neo4j.db}") final String db){
        this.uri = uri;
        this.userName = userName;
        this.password = password;
        this.database = db;
        this.driver = newConnection();
    }


    private final Driver newConnection(){
        final Driver res = GraphDatabase.driver(uri, AuthTokens.basic(userName, password));
        res.verifyConnectivity();
        return res;
    }

    private final Map<String, List<String>> deepEnrich(final Map<String, List<String>> sourceNode,
                                                       final List<String> joinOnList,
                                                       final int maxDepth){
        final String sourceNodeId = sourceNode.get("_id").get(0);
        final Map<String, List<String>> result = shallowEnrich(sourceNode);
        if (joinOnList.isEmpty())
            return result;

        for(final String joinOn : joinOnList){
            if (result.containsKey(joinOn)){
                final String query = String.format("""
                match (n)<-[:RELATED]-(src:ROW_SOURCE) where elementId(n) = '%s'
                match (src)-[:RELATED]->(:%s)<-[:RELATED]-(src1)
                match (src1)-[:RELATED]->(r)
                return r, src1;
                """, sourceNodeId, joinOn);
                final List<DataRecord> recordLst = new ArrayList<>(maxDepth);
                try (final var session = driver.session(SessionConfig.builder().withDatabase(database).build())) {
                    final Result queryResult = session.run(query);
                    final HashSet<String> idCounter = new HashSet<>(maxDepth * 2);
                    while (queryResult.hasNext()){
                        final Record record = queryResult.next();
                        final String srcId = record.get("src1").asNode().elementId();
                        final Node node = record.get("r").asNode();
                        idCounter.add(srcId);
                        if (idCounter.size() > maxDepth)
                            break;

                        String label = "";
                        for(final String l : node.labels()){
                            label = l;
                            break;
                        }
                        recordLst.add(new DataRecord(label, node.get("value").asString()));
                    }
                }catch (final Throwable cause){
                    logger.error("Failed to run deep enrich query. Query: {}. Cause:", query, cause);
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to run deep enrich query.", cause);
                }
                for(final DataRecord r : recordLst){
                    final List<String> rLst = result.get(r.name);
                    if (rLst == null){
                        result.put(r.name, listOf(r.value));
                    }else{
                        if (!rLst.contains(r.value)){
                            rLst.add(r.value);
                        }
                    }
                }
            }
        }
        return result;
    }
    private final Map<String, List<String>> shallowEnrich(final Map<String, List<String>> sourceNode){
        final String sourceNodeId = sourceNode.get("_id").get(0);
        final String query = String.format("""
                match (n)<-[:RELATED]-(src:ROW_SOURCE) where elementId(n) = '%s'
                match (src)-[:RELATED]->(r)
                return r;
                """, sourceNodeId);
        final List<DataRecord> resultList;
        try (final var session = driver.session(SessionConfig.builder().withDatabase(database).build())) {
            resultList = session.run(query).stream().map(record -> {
                final Node node = record.get("r").asNode();
                String label = "";
                for(final String l : node.labels()){
                    label = l;
                    break;
                }

                return new DataRecord(label, node.get("value").asString());
            }).collect(Collectors.toList());
        }catch (final Throwable cause){
            logger.error("Failed to run enrich query. Query: {}. Cause:", query, cause);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to run enrich query.", cause);
        }
        final Map<String, List<String>> result = new HashMap<>(resultList.size() * 2);
        result.put("_id", listOf(sourceNodeId));
        for(final DataRecord r : resultList){
            result.put(r.name, listOf(r.value));
        }
        return result;
    }
    public static enum EnrichmentMethod{
        SHALLOW, DEEP, NONE
    }

    private static final List<String> listOf(final String ...args){
        final List<String> res = new ArrayList<>(args.length + 20);
        res.addAll(Arrays.asList(args));
        return res;
    }

    public final List<Map<String, List<String>>> search(final String recordType,
                                                        final String filter,
                                                        final FilterType filterType,
                                                        final EnrichmentMethod enrichmentMethod,
                                                        final List<String> joinOn,
                                                        final int maxDepth,
                                                        final int skip,
                                                        final int limit){
        final String query = String.format("""
                match (n%s)
                where %s not n:ROW_SOURCE
                return n skip %d limit %d;
                """, hasContent(recordType)?":" + recordType.trim():"",
                buildFilter(filter, filterType),
                skip, limit);
        final List<Map<String, List<String>>> result;
        try (final var session = driver.session(SessionConfig.builder().withDatabase(database).build())) {
            result = session.run(query, Map.of("filter", hasContent(filter)?filter.trim():"")).stream().map(record -> {
                final Node node = record.get("n").asNode();
                String label = "";
                for(final String l : node.labels()){
                    label = l;
                    break;
                }
                return Map.of(
                        "_id", listOf(node.elementId()),
                        label, listOf(node.get("value").asString())
                        );
            }).collect(Collectors.toList());
        }catch (final Throwable cause){
            logger.error("Failed to run search query. Query: {}. Cause:", query, cause);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Failed to run search query.", cause);
        }
        if (enrichmentMethod == EnrichmentMethod.NONE) {
            return result;
        }
        if (enrichmentMethod == EnrichmentMethod.SHALLOW){
            return result.stream().map(this::shallowEnrich).collect(Collectors.toList());
        }
        if (enrichmentMethod == EnrichmentMethod.DEEP){
            return result.stream().map(r-> deepEnrich(r, joinOn, maxDepth)).collect(Collectors.toList());
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported enrichment method: '" + enrichmentMethod + "'.");
    }





    private static final boolean hasContent(final String val){
        if (val == null)
            return false;

        final String trimmed = val.trim();
        if (trimmed.isBlank())
            return false;

        return true;
    }
    private static final String buildFilter(final String filterStr, final FilterType filterType){
        if (!hasContent(filterStr))
            return "";

        final FilterType filter = filterType == null?FilterType.EQUALS:filterType;
        switch (filter){
            case EQUALS -> {
                return "n.value = $filter and";
            }
            case STARTS_WITH -> {
                return "n.value starts with $filter and";
            }
            case ENDS_WITH -> {
                return "n.value ends with $filter and";
            }
            case CONTAINS -> {
                return "n.value contains $filter and";
            }
            default -> {
                return "";
            }
        }
    }

    public static enum FilterType{
        STARTS_WITH, ENDS_WITH, CONTAINS, EQUALS, NONE
    }

    public final UploadResponse createUpload(final UploadResponse upload){
        final String query = "CREATE (u:UploadTracking {uploadId: $uploadId, fileName: $fileName, dataType: $dataType, mappings: $mappings, isComplete: $isComplete, processed: $processed, outOf: $outOf});";
        logger.info("Creating upload record: " + query);
        try (final var session = driver.session(SessionConfig.builder().withDatabase(database).build())) {
            session.executeWrite(tx-> tx.run(query, Map.of("uploadId", upload.uploadId,
                    "fileName", upload.fileName,
                    "dataType", upload.dataType.toString(),
                    "mappings", JSON.toJson(upload.mappings),
                    "isComplete", upload.isComplete,
                    "processed", upload.processed,
                    "outOf", upload.outOf)).consume());
        }catch (final Throwable cause){
            logger.error("Creating upload record failed. Query: {}. Cause:", query, cause);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed create upload record.", cause);
        }
        return getUploadById(upload.uploadId);
    }

    public final UploadResponse getUploadById(final String id){
        final String query = "MATCH (n:UploadTracking) WHERE n.uploadId=$id RETURN n;";
        final Record record;
        try (final var session = driver.session(SessionConfig.builder().withDatabase(database).build())) {
            final List<Record> lst = session.run(query, Map.of("id", id)).list();
            if (lst.isEmpty())
                return null;
            record = lst.get(0);
        }catch (final Throwable cause){
            logger.error("Failed to find uploads. Query: {}. Cause:", query, cause);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to find uploads", cause);
        }

        final org.neo4j.driver.Value v = record.get("n");
        final UploadResponse response = new UploadResponse(
                v.get("fileName").asString(),
                v.get("uploadId").asString(),
                JSON.fromJson(v.get("mappings").asString(), ListOfDataMappings.class),
                DataType.valueOf(v.get("dataType").asString()),
                v.get("isComplete").asBoolean(),
                v.get("processed").asLong(),
                v.get("outOf").asLong());
        if(!v.get("error").isNull()){
            response.error = v.get("error").asString();
        }
        return response;
    }

    public final void updateUploadProgress(final String id, final long progress){
        final String query = "MATCH (n:UploadTracking) WHERE n.uploadId=$id SET n.processed= $processed";
        try (final var session = driver.session(SessionConfig.builder().withDatabase(database).build())) {
            session.executeWrite(tx-> tx.run(query, Map.of("id", id, "processed", progress)).consume());
        }catch (final Throwable cause){
            logger.error("Failed to update progress. Query: {}. Cause:", query, cause);
        }
    }

    public final void completeUploadProgress(final String id){
        final String query = "MATCH (n:UploadTracking) WHERE n.uploadId=$id SET n.isComplete=true";

        try (final var session = driver.session(SessionConfig.builder().withDatabase(database).build())) {
            session.executeWrite(tx-> tx.run(query, Map.of("id", id)).consume());
        }catch (final Throwable cause){
            logger.error("Failed to complete progress. Query: {}. Cause:", query, cause);
        }
    }

    public final void completeUploadProgressWithError(final String id, final String error){
        completeUploadProgress(id);
        final String query = "MATCH (n:UploadTracking) WHERE n.uploadId=$id SET n.error=$error";
        try (final var session = driver.session(SessionConfig.builder().withDatabase(database).build())) {
            session.executeWrite(tx-> tx.run(query, Map.of("id", id, "error", error)).consume());
        }catch (final Throwable cause){
            logger.error("Failed to complete progress. Query: {}. Cause:", query, cause);
        }
    }

    public final List<String> getMappings(){
        final String query = "MATCH (:COLUMNS_ROOT_NODE {value:'COLUMNS_ROOT_NODE'})-[:NODES]->(n:COLUMNS_NODE) RETURN n;";
        final List<String> result;
        try (final var session = driver.session(SessionConfig.builder().withDatabase(database).build())) {
            result = session.run(query).stream().map(r-> r.get("n").asNode().get("value").asString()).collect(Collectors.toList());
        }catch (final Throwable cause){
            logger.error("Failed to fetch nodes. Query: {}. Cause:", query, cause);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed ensure index", cause);
        }
        return result;
    }
    public final void saveMappings(final List<DataMapping> mappings){
        final String query1 = "MERGE (n:COLUMNS_ROOT_NODE {value:'COLUMNS_ROOT_NODE'})";
        try (final var session = driver.session(SessionConfig.builder().withDatabase(database).build())) {
            session.executeWrite(tx-> tx.run(query1).consume());
        }catch (final Throwable cause){
            logger.error("Failed merging columns root node. Query: {}. Cause:", query1, cause);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed ensure index", cause);
        }
        final StringBuilder sb = new StringBuilder(120);
        for(final DataMapping m : mappings){
            sb.append("MERGE (:COLUMNS_ROOT_NODE {value:'COLUMNS_ROOT_NODE'})-[:NODES]->(:COLUMNS_NODE {value: '").append(m.name).append("'})").append('\n');
        }
        final String query2 = sb.toString();
        try (final var session = driver.session(SessionConfig.builder().withDatabase(database).build())) {
            session.executeWrite(tx-> tx.run(query2).consume());
        }catch (final Throwable cause){
            logger.error("Failed merging columns node. Query: {}. Cause:", query2, cause);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed ensure index", cause);
        }
    }



    public final void ensureIndexes(final List<DataMapping> mappings){
        final String metaQuery1 = "CREATE INDEX iColumnsRootNodes IF NOT EXISTS FOR (n:COLUMNS_ROOT_NODE) ON (n.value)";
        final String metaQuery2 = "CREATE INDEX iColumnsNodes IF NOT EXISTS FOR (n:COLUMNS_NODE) ON (n.value)";
        logger.info("Ensuring index: " + metaQuery1);
        try (final var session = driver.session(SessionConfig.builder().withDatabase(database).build())) {
            session.executeWrite(tx-> tx.run(metaQuery1).consume());
        }catch (final Throwable cause){
            logger.error("Index creation failed. Query: {}. Cause:", metaQuery1, cause);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed ensure index", cause);
        }
        logger.info("Ensuring index: " + metaQuery2);
        try (final var session = driver.session(SessionConfig.builder().withDatabase(database).build())) {
            session.executeWrite(tx-> tx.run(metaQuery2).consume());
        }catch (final Throwable cause){
            logger.error("Index creation failed. Query: {}. Cause:", metaQuery2, cause);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed ensure index", cause);
        }
        final String uploadsQuery = "CREATE INDEX iUploadTracking IF NOT EXISTS FOR (n:UploadTracking) ON (n.uploadId)";
        logger.info("Ensuring index: " + uploadsQuery);
        try (final var session = driver.session(SessionConfig.builder().withDatabase(database).build())) {
            session.executeWrite(tx-> tx.run(uploadsQuery).consume());
        }catch (final Throwable cause){
            logger.error("Index creation failed. Query: {}. Cause:", uploadsQuery, cause);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed ensure index", cause);
        }
        final String relationshipQuery = "CREATE INDEX iRowSource IF NOT EXISTS FOR (n:ROW_SOURCE) ON (n.value)";
        logger.info("Ensuring index: " + relationshipQuery);
        try (final var session = driver.session(SessionConfig.builder().withDatabase(database).build())) {
            session.executeWrite(tx-> tx.run(relationshipQuery).consume());
        }catch (final Throwable cause){
            logger.error("Index creation failed. Query: {}. Cause:", relationshipQuery, cause);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed ensure index", cause);
        }
        for(final DataMapping dataMapping : mappings){
            final String query = "CREATE TEXT INDEX i" + dataMapping.name.substring(0, 1).toUpperCase() + dataMapping.name.substring(1) + " IF NOT EXISTS FOR (n:" + dataMapping.name + ") ON (n.value)";
            logger.info("Ensuring index: " + query);
            try (final var session = driver.session(SessionConfig.builder().withDatabase(database).build())) {
                session.executeWrite(tx-> tx.run(query).consume());
            }catch (final Throwable cause){
                logger.error("Index creation failed. Query: {}. Cause:", query, cause);
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed ensure index", cause);
            }
        }
    }


    public final void saveRecord(final List<DataRecord> records){
        if (records.isEmpty())
            return;

        final Map.Entry<String, Map<String,Object>> query = buildQuery(records);
        final long startTime = System.currentTimeMillis();
        try (final var session = driver.session(SessionConfig.builder().withDatabase(database).build())) {
            session.executeWrite(tx-> tx.run(query.getKey(), query.getValue()).consume());
        }catch (final Throwable cause){
            logger.error("DB write failed. Query: {}. Cause:", query.getKey(), cause);
            return;
        }
        logger.info("Query: {} | executed in {} milliseconds.", query.getKey(), (System.currentTimeMillis() - startTime));
    }

    public static final class ListOfDataMappings extends ArrayList<DataMapping> implements List<DataMapping>{

    }
    private final Map.Entry<String, Map<String,Object>> buildQuery(final List<DataRecord> records_unsorted){
        final Map<String,Object> params = new HashMap<>(records_unsorted.size() * 2);
        final List<DataRecord> records = records_unsorted.stream().sorted(Comparator.comparing(o -> o.name)).toList();

        final StringBuilder stringBuilder = new StringBuilder((records.size() + 2) * 100);
        for (int i = 0; i < records.size(); i++) {
            final DataRecord r = records.get(i);
            final String index = toIndex(i);
            final String paramName = "p_" + index;
            params.put(paramName, r.value);
            stringBuilder.append("MERGE (n").append(index).append(":").append(r.name).append(" {value: $").append(paramName).append("})").append('\n');
        }

        final String paramName = "row";
        params.put(paramName, Signature.getSignature(records));
        stringBuilder.append("MERGE (row:ROW_SOURCE {value: $").append(paramName).append("})").append('\n');

        for (int i = 0; i < records.size(); i++) {
            stringBuilder.append("MERGE (n").append(toIndex(i)).append(")").append("<-[:RELATED]-(row)").append('\n');
        }

        return new Map.Entry<>() {
            @Override
            public final String getKey() {
                return stringBuilder.toString();
            }
            @Override
            public final Map<String, Object> getValue() {
                return params;
            }
            @Override
            public final Map<String, Object> setValue(Map<String, Object> value) {
                return null;
            }
        };
    }

    private static final String toIndex(final int index){
        return Integer.toString(index);
    }
    @PreDestroy
    private final void close(){
        driver.close();
    }
}
