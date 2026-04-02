package com.aiready.job.kafka;

import com.aiready.global.sse.SseService;
import com.aiready.job.JobService;
import com.aiready.job.dto.JobStatusEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class JobResultConsumer {

    private final JobService jobService;
    private final SseService sseService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "eval-result", groupId = "backend-result-consumer")
    public void consume(String message) {
        try {
            Map<String, Object> result = objectMapper.readValue(message, Map.class);
            Long jobId    = Long.valueOf(result.get("job_id").toString());
            String status = result.get("status").toString();
            String s3Key  = (String) result.get("result_s3_key");

            jobService.updateJobResult(jobId, status, s3Key);
            sseService.push(jobId, new JobStatusEvent(jobId, status, s3Key));
        } catch (Exception e) {
            log.error("eval-result 처리 실패", e);
        }
    }
}
