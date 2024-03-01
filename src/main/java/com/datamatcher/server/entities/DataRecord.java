package com.datamatcher.server.entities;

import java.util.Objects;
import java.util.StringJoiner;

public final class DataRecord {
    public String name;
    public String value;
    public DataRecord(final String name, final String value){
        this.name = name;
        this.value = value;
    }
    public DataRecord(){}

    @Override
    public final boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof DataRecord record)) return false;
        if (!Objects.equals(name, record.name)) return false;
        return Objects.equals(value, record.value);
    }

    @Override
    public final int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }
    @Override
    public final String toString() {
        return new StringJoiner(", ", DataRecord.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("value='" + value + "'")
                .toString();
    }
}
