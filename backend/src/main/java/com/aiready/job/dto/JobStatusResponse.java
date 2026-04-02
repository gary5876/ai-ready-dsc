package com.aiready.job.dto;

import com.aiready.job.EvalJob;

public record JobStatusResponse(
        Long jobId,
        String status,
        int progress,
        String resultS3Key,
        String errorMsg
) {
    public static JobStatusResponse of(EvalJob job) {
        return new JobStatusResponse(
                job.getId(),
                job.getStatus().name(),
                job.getProgress(),
                job.getResultS3Key(),
                job.getErrorMsg()
        );
    }
}
