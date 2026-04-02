package com.aiready.job.dto;

public record JobStatusEvent(Long jobId, String status, String resultS3Key) {}
