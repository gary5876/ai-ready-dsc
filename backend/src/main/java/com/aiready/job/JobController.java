package com.aiready.job;

import com.aiready.global.sse.SseService;
import com.aiready.job.dto.JobStatusResponse;
import com.aiready.job.dto.JobSubmitRequest;
import com.aiready.job.kafka.JobProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    private final JobProducer jobProducer;
    private final SseService sseService;

    @PostMapping
    public ResponseEntity<JobStatusResponse> submitJob(@RequestBody JobSubmitRequest req) {
        EvalJob job = jobService.createJob(req.datasetId(), req.userId());
        jobProducer.send(job);
        return ResponseEntity.ok(JobStatusResponse.of(job));
    }

    @GetMapping(value = "/{jobId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamJobStatus(@PathVariable Long jobId) {
        return sseService.createEmitter(jobId);
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<JobStatusResponse> getJobStatus(@PathVariable Long jobId) {
        return ResponseEntity.ok(jobService.getStatus(jobId));
    }

    @GetMapping
    public ResponseEntity<List<JobStatusResponse>> listJobs(@RequestParam Long userId) {
        return ResponseEntity.ok(jobService.listByUser(userId));
    }
}
