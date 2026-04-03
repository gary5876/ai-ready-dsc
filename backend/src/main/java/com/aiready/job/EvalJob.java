package com.aiready.job;

import com.aiready.dataset.Dataset;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "eval_jobs")
@Getter
@NoArgsConstructor
public class EvalJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long datasetId;

    @Column(nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    private JobStatus status = JobStatus.PENDING;

    private int progress = 0;
    private String errorMsg;
    @Column(name = "result_s3_key")
    private String resultS3Key;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "datasetId", insertable = false, updatable = false)
    private Dataset dataset;

    public EvalJob(Long datasetId, Long userId) {
        this.datasetId = datasetId;
        this.userId = userId;
    }

    public void updateResult(String status, String resultS3Key) {
        this.status = JobStatus.valueOf(status);
        this.resultS3Key = resultS3Key;
        this.updatedAt = LocalDateTime.now();
    }

    public enum JobStatus {
        PENDING, EVALUATING, SCORING, GENERATING_REPORT, DONE, FAILED
    }
}
