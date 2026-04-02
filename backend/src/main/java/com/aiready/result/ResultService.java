package com.aiready.result;

import com.aiready.result.dto.EvalResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ResultService {

    private final EvalResultRepository evalResultRepository;

    public List<EvalResultResponse> getResultsByJob(Long jobId) {
        return evalResultRepository.findByJobId(jobId)
                .stream().map(EvalResultResponse::of).toList();
    }
}
