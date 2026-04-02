package com.aiready.dataset.dto;

import com.aiready.dataset.Dataset;

import java.time.LocalDateTime;

public record DatasetResponse(
        Long id,
        String name,
        String s3Key,
        Integer rowCount,
        Integer colCount,
        LocalDateTime createdAt
) {
    public static DatasetResponse of(Dataset d) {
        return new DatasetResponse(d.getId(), d.getName(), d.getS3Key(),
                d.getRowCount(), d.getColCount(), d.getCreatedAt());
    }
}
