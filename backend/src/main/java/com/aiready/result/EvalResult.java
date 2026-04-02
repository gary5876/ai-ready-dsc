package com.aiready.result;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "eval_results")
@Getter
@NoArgsConstructor
public class EvalResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long jobId;

    @Column(nullable = false, length = 100)
    private String criteriaName;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal score;

    @Column(nullable = false, precision = 4, scale = 3)
    private BigDecimal weight;

    @Column(columnDefinition = "TEXT")
    private String detail;

    public EvalResult(Long jobId, String criteriaName, BigDecimal score, BigDecimal weight, String detail) {
        this.jobId = jobId;
        this.criteriaName = criteriaName;
        this.score = score;
        this.weight = weight;
        this.detail = detail;
    }
}
