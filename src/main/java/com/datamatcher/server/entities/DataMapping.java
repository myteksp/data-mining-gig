package com.datamatcher.server.entities;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

public final class DataMapping {
    public String name;
    public String path;
    public List<Transformation> transformations;

    public DataMapping(final String name, final String path, final List<Transformation> transformations){
        this.name = name;
        this.path = path;
        this.transformations = transformations;
    }
    public DataMapping(){}

    public static final List<DataMapping> parseMappings(final List<String> mappings){
        if (mappings.isEmpty()){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mappings are empty");
        }
        final List<DataMapping> res = new ArrayList<>(mappings.size());
        for(final String mapping : mappings){
            final String[] split = mapping.split(":");
            if (split.length < 2){
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid mapping: '" + mapping + "'.");
            }
            final List<DataMapping.Transformation> transformations = new ArrayList<>(5);
            for (int i = 2; i < split.length; i++) {
                try {
                    transformations.add(DataMapping.Transformation.valueOf(split[i].toUpperCase()));
                }catch (final Throwable t){
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid mapping: '" + mapping + "'.");
                }
            }
            res.add(new DataMapping(split[0], split[1], transformations));
        }
        return res;
    }

    @Override
    public final boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof DataMapping that)) return false;

        if (!Objects.equals(name, that.name)) return false;
        if (!Objects.equals(path, that.path)) return false;
        return Objects.equals(transformations, that.transformations);
    }

    @Override
    public final int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (path != null ? path.hashCode() : 0);
        result = 31 * result + (transformations != null ? transformations.hashCode() : 0);
        return result;
    }

    @Override
    public final String toString() {
        return new StringJoiner(", ", DataMapping.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("path='" + path + "'")
                .add("transformations=" + transformations)
                .toString();
    }

    public static enum Transformation{
        TLC, TUC, TRIM, NRM
    }
}
