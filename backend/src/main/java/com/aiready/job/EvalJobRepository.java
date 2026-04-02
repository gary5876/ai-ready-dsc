package com.aiready.job;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvalJobRepository extends JpaRepository<EvalJob, Long> {
    List<EvalJob> findByUserId(Long userId);
}
