package com.aiready.result;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvalResultRepository extends JpaRepository<EvalResult, Long> {
    List<EvalResult> findByJobId(Long jobId);
}
