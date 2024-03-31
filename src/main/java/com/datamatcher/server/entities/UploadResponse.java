package com.datamatcher.server.entities;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.StringJoiner;

public final class UploadResponse {
    public String fileName;
    public String uploadId;
    public DataType dataType;
    public List<DataMapping> mappings;
    public String error;
    public boolean isComplete;
    public long processed;
    public long outOf;
    public int rowsCount;

    public UploadResponse(final String fileName,
                          final String uploadId,
                          final List<DataMapping> mappings,
                          final DataType dataType,
                          final boolean isComplete,
                          final long processed,
                          final long outOf,
                          final int rowsCount){
        this.fileName = fileName;
        this.mappings = mappings;
        this.uploadId = uploadId;
        this.dataType = dataType;
        this.isComplete = isComplete;
        this.processed = processed;
        this.outOf = outOf;
        this.rowsCount = rowsCount;
        this.error = null;
    }
    public UploadResponse(){
        this.error = null;
    }

    @Override
    public final boolean equals(final Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UploadResponse that = (UploadResponse) o;
        return isComplete == that.isComplete && processed == that.processed && outOf == that.outOf && rowsCount == that.rowsCount && Objects.equals(fileName, that.fileName) && Objects.equals(uploadId, that.uploadId) && dataType == that.dataType && Objects.equals(mappings, that.mappings) && Objects.equals(error, that.error);
    }

    @Override
    public final int hashCode() {
        return Objects.hash(fileName, uploadId, dataType, mappings, error, isComplete, processed, outOf, rowsCount);
    }

    @Override
    public final String toString() {
        return "UploadResponse{" +
                "fileName='" + fileName + '\'' +
                ", uploadId='" + uploadId + '\'' +
                ", dataType=" + dataType +
                ", mappings=" + mappings +
                ", error='" + error + '\'' +
                ", isComplete=" + isComplete +
                ", processed=" + processed +
                ", outOf=" + outOf +
                ", rowsCount=" + rowsCount +
                '}';
    }
}
