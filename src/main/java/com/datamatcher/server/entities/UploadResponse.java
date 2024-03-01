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

    public UploadResponse(final String fileName,
                          final String uploadId,
                          final List<DataMapping> mappings,
                          final DataType dataType,
                          final boolean isComplete,
                          final long processed,
                          final long outOf){
        this.fileName = fileName;
        this.mappings = mappings;
        this.uploadId = uploadId;
        this.dataType = dataType;
        this.isComplete = isComplete;
        this.processed = processed;
        this.outOf = outOf;
        this.error = null;
    }
    public UploadResponse(){
        this.error = null;
    }

    @Override
    public final boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof UploadResponse response)) return false;
        if (!Objects.equals(fileName, response.fileName)) return false;
        if (!Objects.equals(uploadId, response.uploadId)) return false;
        if (dataType != response.dataType) return false;
        return Objects.equals(mappings, response.mappings);
    }

    @Override
    public final int hashCode() {
        int result = fileName != null ? fileName.hashCode() : 0;
        result = 31 * result + (uploadId != null ? uploadId.hashCode() : 0);
        result = 31 * result + (dataType != null ? dataType.hashCode() : 0);
        result = 31 * result + (mappings != null ? mappings.hashCode() : 0);
        return result;
    }

    @Override
    public final String toString() {
        return new StringJoiner(", ", UploadResponse.class.getSimpleName() + "[", "]")
                .add("fileName='" + fileName + "'")
                .add("uploadId='" + uploadId + "'")
                .add("dataType=" + dataType)
                .add("mappings=" + mappings)
                .toString();
    }
}
