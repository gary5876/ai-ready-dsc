package com.aiready.result;

import com.aiready.result.dto.EvalResultResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class ResultController {

    private final ResultService resultService;

    @GetMapping("/{jobId}/result")
    public ResponseEntity<List<EvalResultResponse>> getResult(@PathVariable Long jobId) {
        return ResponseEntity.ok(resultService.getResultsByJob(jobId));
    }
}
