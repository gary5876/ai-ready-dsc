package com.aiready.job;

import com.aiready.job.dto.JobStatusResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class JobService {

    private final EvalJobRepository evalJobRepository;

    @Transactional
    public EvalJob createJob(Long datasetId, Long userId) {
        EvalJob job = new EvalJob(datasetId, userId);
        return evalJobRepository.save(job);
    }

    public JobStatusResponse getStatus(Long jobId) {
        EvalJob job = evalJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
        return JobStatusResponse.of(job);
    }

    public List<JobStatusResponse> listByUser(Long userId) {
        return evalJobRepository.findByUserId(userId)
                .stream().map(JobStatusResponse::of).toList();
    }

    @Transactional
    public void updateJobResult(Long jobId, String status, String resultS3Key) {
        EvalJob job = evalJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job not found: " + jobId));
        job.updateResult(status, resultS3Key);
    }
}
