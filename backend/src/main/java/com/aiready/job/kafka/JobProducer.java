package com.aiready.job.kafka;

import com.aiready.job.EvalJob;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class JobProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void send(EvalJob job) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                "job_id",     job.getId(),
                "dataset_id", job.getDatasetId(),
                "s3_key",     job.getDataset().getS3Key(),
                "user_id",    job.getUserId()
            ));
            kafkaTemplate.send("eval-request", String.valueOf(job.getId()), payload);
        } catch (Exception e) {
            throw new RuntimeException("Kafka 발행 실패", e);
        }
    }
}
