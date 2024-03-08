package com.datamatcher.server.repositories;

import com.datamatcher.server.entities.DataRecord;
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
public class SearchRepo {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Driver driver;
    private final String database;
    private final String userName;
    private final String password;
    private final String uri;
    public SearchRepo(@Value("${neo4j.uri}") final String uri,
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
                where %s not n:ROW_SOURCE and not n:UploadTracking and not n:COLUMNS_ROOT_NODE and not n:COLUMNS_NODE
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

        final FilterType filter = filterType == null? FilterType.EQUALS:filterType;
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
}
