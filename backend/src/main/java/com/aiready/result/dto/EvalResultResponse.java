package com.aiready.result.dto;

import com.aiready.result.EvalResult;

import java.math.BigDecimal;

public record EvalResultResponse(
        Long id,
        Long jobId,
        String criteriaName,
        BigDecimal score,
        BigDecimal weight,
        String detail
) {
    public static EvalResultResponse of(EvalResult r) {
        return new EvalResultResponse(r.getId(), r.getJobId(), r.getCriteriaName(),
                r.getScore(), r.getWeight(), r.getDetail());
    }
}
