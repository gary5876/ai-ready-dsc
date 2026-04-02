package com.aiready.dataset;

import com.aiready.dataset.dto.DatasetResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DatasetService {

    private final DatasetRepository datasetRepository;
    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    public DatasetResponse upload(Long userId, String name, MultipartFile file) throws IOException {
        String s3Key = "datasets/" + UUID.randomUUID() + "/" + file.getOriginalFilename();

        s3Client.putObject(
                PutObjectRequest.builder().bucket(bucket).key(s3Key).build(),
                RequestBody.fromBytes(file.getBytes())
        );

        Dataset dataset = new Dataset(userId, name, s3Key, null, null);
        datasetRepository.save(dataset);
        return DatasetResponse.of(dataset);
    }

    public List<DatasetResponse> listByUser(Long userId) {
        return datasetRepository.findByUserId(userId)
                .stream().map(DatasetResponse::of).toList();
    }
}
