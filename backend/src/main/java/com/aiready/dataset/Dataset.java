package com.aiready.dataset;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "datasets")
@Getter
@NoArgsConstructor
public class Dataset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 512)
    private String s3Key;

    private Integer rowCount;
    private Integer colCount;

    private LocalDateTime createdAt = LocalDateTime.now();

    public Dataset(Long userId, String name, String s3Key, Integer rowCount, Integer colCount) {
        this.userId = userId;
        this.name = name;
        this.s3Key = s3Key;
        this.rowCount = rowCount;
        this.colCount = colCount;
    }
}
