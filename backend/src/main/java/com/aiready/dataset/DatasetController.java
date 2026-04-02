package com.aiready.dataset;

import com.aiready.dataset.dto.DatasetResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/datasets")
@RequiredArgsConstructor
public class DatasetController {

    private final DatasetService datasetService;

    // TODO: @AuthenticationPrincipal로 교체 — 현재는 userId 파라미터로 임시 처리
    @PostMapping("/upload")
    public ResponseEntity<DatasetResponse> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam("name") String name,
            @RequestParam("userId") Long userId) throws IOException {
        return ResponseEntity.ok(datasetService.upload(userId, name, file));
    }

    @GetMapping
    public ResponseEntity<List<DatasetResponse>> list(@RequestParam("userId") Long userId) {
        return ResponseEntity.ok(datasetService.listByUser(userId));
    }
}
