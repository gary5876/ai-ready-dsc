# AI-Ready Data 평가 플랫폼 — 구현 스펙

## 목표
- 사용자가 데이터셋을 업로드하면 여러 평가기준으로 점수를 내고,
  그 점수를 LLM이 비전문가 수준의 보고서로 변환해 반환한다.
- 평가 로직(EvalCriteria)은 다른 팀원이 구현하므로 **인터페이스만 고정**하고 Mock으로 대체한다.
- 많은 사용자가 동시에 요청해도 버티도록 **비동기 파이프라인**을 설계한다.

---

## 기술 스택

| 역할 | 기술 |
|------|------|
| 프론트엔드 | React (Vite) |
| 메인 백엔드 | Spring Boot 3 (Java 21) |
| AI 워커 | FastAPI (Python 3.11) |
| 메시지 큐 | Apache Kafka |
| DB | MySQL 8 |
| 파일 저장 | Amazon S3 |
| LLM | OpenAI API (gpt-4o) |
| 컨테이너 | Docker Compose |
| 모니터링 | Prometheus + Grafana |
| CI/CD | GitHub Actions |

---

## 프로젝트 디렉토리 구조

기능(controller/service/entity)이 아닌 **도메인(dataset/job/result)** 기준으로 묶는다.
같은 도메인의 코드는 한 폴더 안에서 바로 찾을 수 있어야 한다.

```
ai-ready-platform/
├── docker-compose.yml
├── .env.example
│
├── backend/                  # Spring Boot
│   ├── build.gradle
│   ├── Dockerfile
│   └── src/main/java/com/aiready/
│       ├── AiReadyApplication.java
│       │
│       ├── global/                            # 도메인에 속하지 않는 공통
│       │   ├── config/
│       │   │   ├── KafkaConfig.java
│       │   │   ├── SecurityConfig.java
│       │   │   └── S3Config.java
│       │   ├── exception/
│       │   │   └── GlobalExceptionHandler.java
│       │   └── sse/
│       │       └── SseService.java            # SSE emitter 관리
│       │
│       ├── dataset/                           # 도메인: 데이터셋 관리
│       │   ├── Dataset.java                   # Entity
│       │   ├── DatasetRepository.java
│       │   ├── DatasetService.java
│       │   ├── DatasetController.java
│       │   └── dto/
│       │       ├── DatasetUploadRequest.java
│       │       └── DatasetResponse.java
│       │
│       ├── job/                               # 도메인: 평가 작업
│       │   ├── EvalJob.java                   # Entity
│       │   ├── EvalJobRepository.java
│       │   ├── JobService.java
│       │   ├── JobController.java
│       │   ├── kafka/
│       │   │   ├── JobProducer.java           # eval-request 발행
│       │   │   └── JobResultConsumer.java     # eval-result 수신 → SSE push
│       │   └── dto/
│       │       ├── JobSubmitRequest.java
│       │       └── JobStatusResponse.java
│       │
│       └── result/                            # 도메인: 평가 결과
│           ├── EvalResult.java                # Entity
│           ├── EvalResultRepository.java
│           ├── ResultService.java
│           ├── ResultController.java
│           └── dto/
│               └── EvalResultResponse.java
│
├── worker/                   # FastAPI
│   ├── requirements.txt
│   ├── Dockerfile
│   ├── main.py
│   │
│   ├── global/                                # 공통 인프라
│   │   ├── db.py                              # DB 연결
│   │   ├── s3.py                              # S3 클라이언트
│   │   └── messaging/
│   │       ├── consumer.py                    # Kafka consumer
│   │       └── producer.py                    # Kafka producer
│   │
│   ├── evaluation/                            # 도메인: 평가 실행
│   │   ├── runner.py                          # 파이프라인 오케스트레이터
│   │   ├── scoring.py                         # 총점 계산
│   │   └── criteria/
│   │       ├── base.py                        # EvalCriteria 인터페이스 (수정 금지)
│   │       └── mock_criteria.py               # Mock → 팀원이 교체
│   │
│   └── report/                                # 도메인: 보고서 생성
│       └── generator.py                       # LLM 호출 (1회)
│
└── frontend/                 # React
    ├── package.json
    ├── Dockerfile
    └── src/
        ├── App.jsx
        ├── pages/
        │   ├── UploadPage.jsx
        │   ├── JobListPage.jsx
        │   └── ResultPage.jsx
        └── components/
            ├── ProgressBar.jsx                # SSE 연결
            └── ReportViewer.jsx
```

---

## docker-compose.yml

```yaml
version: '3.9'

services:
  zookeeper:
    image: confluentinc/cp-zookeeper:7.5.0
    environment:
      ZOOKEEPER_CLIENT_PORT: 2181

  kafka:
    image: confluentinc/cp-kafka:7.5.0
    depends_on: [zookeeper]
    environment:
      KAFKA_BROKER_ID: 1
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
      KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
      KAFKA_NUM_PARTITIONS: 4          # FastAPI 인스턴스 수와 맞춤
    ports:
      - "9092:9092"

  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      MYSQL_DATABASE: aiready
    volumes:
      - mysql_data:/var/lib/mysql
    ports:
      - "3306:3306"

  backend:
    build: ./backend
    depends_on: [kafka, mysql]
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/aiready
      SPRING_DATASOURCE_PASSWORD: ${MYSQL_ROOT_PASSWORD}
      KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      AWS_ACCESS_KEY_ID: ${AWS_ACCESS_KEY_ID}
      AWS_SECRET_ACCESS_KEY: ${AWS_SECRET_ACCESS_KEY}
      S3_BUCKET: ${S3_BUCKET}
    ports:
      - "8080:8080"

  worker:
    build: ./worker
    depends_on: [kafka, mysql]
    environment:
      KAFKA_BOOTSTRAP_SERVERS: kafka:9092
      MYSQL_URL: mysql+asyncmy://root:${MYSQL_ROOT_PASSWORD}@mysql:3306/aiready
      OPENAI_API_KEY: ${OPENAI_API_KEY}
      AWS_ACCESS_KEY_ID: ${AWS_ACCESS_KEY_ID}
      AWS_SECRET_ACCESS_KEY: ${AWS_SECRET_ACCESS_KEY}
      S3_BUCKET: ${S3_BUCKET}
    deploy:
      replicas: 4              # Kafka Partition 수와 맞춤 → 동시 4작업 처리

  frontend:
    build: ./frontend
    ports:
      - "3000:80"

  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf
    depends_on: [backend, frontend]

  prometheus:
    image: prom/prometheus
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml

  grafana:
    image: grafana/grafana
    ports:
      - "3001:3000"

volumes:
  mysql_data:
```

---

## MySQL 스키마

```sql
-- flyway migration: V1__init.sql

CREATE TABLE users (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    email      VARCHAR(255) NOT NULL UNIQUE,
    role       ENUM('USER', 'ADMIN') DEFAULT 'USER',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE datasets (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    name        VARCHAR(255) NOT NULL,
    s3_key      VARCHAR(512) NOT NULL,
    row_count   INT,
    col_count   INT,
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE eval_jobs (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    dataset_id    BIGINT NOT NULL,
    user_id       BIGINT NOT NULL,
    status        ENUM('PENDING','EVALUATING','SCORING','GENERATING_REPORT','DONE','FAILED') DEFAULT 'PENDING',
    progress      INT DEFAULT 0,
    error_msg     TEXT,
    result_s3_key VARCHAR(512),
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (dataset_id) REFERENCES datasets(id)
);

CREATE TABLE eval_results (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id        BIGINT NOT NULL,
    criteria_name VARCHAR(100) NOT NULL,
    score         DECIMAL(5,2) NOT NULL,   -- 0.00 ~ 100.00
    weight        DECIMAL(4,3) NOT NULL,   -- 0.000 ~ 1.000
    detail        TEXT,
    FOREIGN KEY (job_id) REFERENCES eval_jobs(id)
);
```

---

## Kafka 토픽 설계

| 토픽 | Partition | 방향 | 설명 |
|------|-----------|------|------|
| `eval-request` | 4 | Spring Boot → FastAPI | 평가 작업 요청 |
| `eval-result` | 4 | FastAPI → Spring Boot | 평가 완료/실패 결과 |

### 메시지 스키마 (JSON)

**eval-request:**
```json
{
  "job_id": 123,
  "dataset_id": 45,
  "s3_key": "datasets/45/raw/data.csv",
  "user_id": 7
}
```

**eval-result:**
```json
{
  "job_id": 123,
  "status": "DONE",
  "result_s3_key": "reports/123/report.json",
  "error_msg": null
}
```

---

## Spring Boot 핵심 구현

### API 엔드포인트

```
POST  /api/datasets/upload       # 데이터셋 업로드 (multipart)
GET   /api/datasets              # 내 데이터셋 목록

POST  /api/jobs                  # 평가 작업 제출 → jobId 즉시 반환
GET   /api/jobs                  # 내 작업 목록
GET   /api/jobs/{jobId}          # 작업 상태 조회
GET   /api/jobs/{jobId}/stream   # SSE: 실시간 진행률 스트리밍
GET   /api/jobs/{jobId}/result   # 완료된 결과 조회
```

### job/JobController.java

```java
// 패키지: com.aiready.job
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;
    private final JobProducer jobProducer;
    private final SseService sseService;

    // 즉시 jobId 반환 — 처리는 Kafka → FastAPI에서 비동기로
    @PostMapping
    public ResponseEntity<JobStatusResponse> submitJob(
            @RequestBody JobSubmitRequest req,
            @AuthenticationPrincipal User user) {
        EvalJob job = jobService.createJob(req.datasetId(), user.getId());
        jobProducer.send(job);
        return ResponseEntity.ok(JobStatusResponse.of(job));
    }

    // SSE 스트림 — FastAPI 완료 시 JobResultConsumer가 push
    @GetMapping(value = "/{jobId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamJobStatus(@PathVariable Long jobId) {
        return sseService.createEmitter(jobId);
    }

    @GetMapping("/{jobId}")
    public ResponseEntity<JobStatusResponse> getJobStatus(@PathVariable Long jobId) {
        return ResponseEntity.ok(jobService.getStatus(jobId));
    }
}
```

### job/kafka/JobProducer.java

```java
// 패키지: com.aiready.job.kafka
@Component
@RequiredArgsConstructor
public class JobProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void send(EvalJob job) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                "job_id",     job.getId(),
                "dataset_id", job.getDatasetId(),
                "s3_key",     job.getDataset().getS3Key(),
                "user_id",    job.getUserId()
            ));
            kafkaTemplate.send("eval-request", String.valueOf(job.getId()), payload);
        } catch (Exception e) {
            throw new RuntimeException("Kafka 발행 실패", e);
        }
    }
}
```

### job/kafka/JobResultConsumer.java

```java
// 패키지: com.aiready.job.kafka
@Component
@RequiredArgsConstructor
public class JobResultConsumer {

    private final JobService jobService;
    private final SseService sseService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "eval-result", groupId = "backend-result-consumer")
    public void consume(String message) {
        try {
            Map<String, Object> result = objectMapper.readValue(message, Map.class);
            Long jobId    = Long.valueOf(result.get("job_id").toString());
            String status = result.get("status").toString();
            String s3Key  = (String) result.get("result_s3_key");

            jobService.updateJobResult(jobId, status, s3Key);
            sseService.push(jobId, new JobStatusEvent(jobId, status, s3Key));
        } catch (Exception e) {
            log.error("eval-result 처리 실패", e);
        }
    }
}
```

### global/sse/SseService.java

```java
// 패키지: com.aiready.global.sse
@Service
public class SseService {

    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter createEmitter(Long jobId) {
        SseEmitter emitter = new SseEmitter(180_000L); // 3분 타임아웃
        emitters.put(jobId, emitter);
        emitter.onCompletion(() -> emitters.remove(jobId));
        emitter.onTimeout(()    -> emitters.remove(jobId));
        return emitter;
    }

    public void push(Long jobId, JobStatusEvent event) {
        SseEmitter emitter = emitters.get(jobId);
        if (emitter == null) return;
        try {
            emitter.send(SseEmitter.event().data(event));
            if ("DONE".equals(event.status()) || "FAILED".equals(event.status())) {
                emitter.complete();
            }
        } catch (IOException e) {
            emitters.remove(jobId);
        }
    }
}
```

---

## FastAPI 핵심 구현

### evaluation/criteria/base.py — 인터페이스 (절대 수정 금지)

```python
# worker/evaluation/criteria/base.py
from abc import ABC, abstractmethod
from dataclasses import dataclass

@dataclass
class CriteriaResult:
    name: str
    score: float      # 0.0 ~ 100.0
    weight: float     # 0.0 ~ 1.0 (전체 합이 1.0이 되어야 함)
    detail: str       # 보고서에 들어갈 한 줄 설명 (한국어)

class EvalCriteria(ABC):
    """
    팀원 구현 계약:
      - 이 클래스를 상속받아 evaluate()를 구현한다.
      - score: 반드시 0.0 ~ 100.0 사이 float
      - detail: 비전문가가 이해할 수 있는 한국어 한 줄 문자열
      - dataset: pandas DataFrame으로 전달됨
    """
    name: str
    weight: float

    @abstractmethod
    def evaluate(self, dataset) -> CriteriaResult:
        pass
```

### evaluation/criteria/mock_criteria.py — Mock 구현

```python
# worker/evaluation/criteria/mock_criteria.py
import random
from .base import EvalCriteria, CriteriaResult

class MockCompletenessEval(EvalCriteria):
    name = "완전성"
    weight = 0.3

    def evaluate(self, dataset) -> CriteriaResult:
        # TODO: 팀원이 실제 로직으로 교체
        score = round(random.uniform(60, 95), 1)
        missing_pct = round(100 - score, 1)
        return CriteriaResult(
            name=self.name, score=score, weight=self.weight,
            detail=f"전체 필드 중 약 {missing_pct}%에서 결측값이 발견되었습니다."
        )

class MockConsistencyEval(EvalCriteria):
    name = "일관성"
    weight = 0.3

    def evaluate(self, dataset) -> CriteriaResult:
        score = round(random.uniform(70, 98), 1)
        return CriteriaResult(
            name=self.name, score=score, weight=self.weight,
            detail=f"데이터 형식 일관성 점수 {score}점. 날짜/숫자 형식 혼재 여부를 확인하세요."
        )

class MockUniquenessEval(EvalCriteria):
    name = "유일성"
    weight = 0.2

    def evaluate(self, dataset) -> CriteriaResult:
        score = round(random.uniform(80, 99), 1)
        dup_pct = round(100 - score, 1)
        return CriteriaResult(
            name=self.name, score=score, weight=self.weight,
            detail=f"중복 행이 약 {dup_pct}% 감지되었습니다."
        )

class MockAccuracyEval(EvalCriteria):
    name = "정확성"
    weight = 0.2

    def evaluate(self, dataset) -> CriteriaResult:
        score = round(random.uniform(65, 95), 1)
        return CriteriaResult(
            name=self.name, score=score, weight=self.weight,
            detail=f"값의 범위와 논리적 유효성 점수 {score}점."
        )

# ──────────────────────────────────────────────────────────────
# 팀원이 실제 구현으로 교체할 때는 이 리스트만 수정한다.
# runner.py / scoring.py / report/generator.py 는 건드리지 않는다.
# ──────────────────────────────────────────────────────────────
CRITERIA_LIST = [
    MockCompletenessEval(),
    MockConsistencyEval(),
    MockUniquenessEval(),
    MockAccuracyEval(),
]
```

### evaluation/runner.py — 파이프라인 오케스트레이터

```python
# worker/evaluation/runner.py
from evaluation.criteria.mock_criteria import CRITERIA_LIST
from evaluation.criteria.base import CriteriaResult
from evaluation.scoring import calculate_total_score
from report.generator import generate_report

async def run_pipeline(job_id: int, s3_key: str, db, s3_client) -> str:
    """
    전체 평가 파이프라인 실행.
    반환값: 보고서가 저장된 S3 key
    """
    try:
        # 1단계: 데이터 로드
        await update_job(db, job_id, status="EVALUATING", progress=5)
        dataset = load_dataset_from_s3(s3_client, s3_key)  # pd.DataFrame

        # 2단계: 평가기준별 실행
        results = []
        for i, criteria in enumerate(CRITERIA_LIST):
            try:
                result = criteria.evaluate(dataset)
                results.append(result)
            except Exception as e:
                # 한 기준 실패해도 전체 중단하지 않음
                results.append(CriteriaResult(
                    name=criteria.name,
                    score=0.0,
                    weight=criteria.weight,
                    detail=f"평가 중 오류 발생: {str(e)}"
                ))
            progress = 5 + int((i + 1) / len(CRITERIA_LIST) * 60)
            await update_job(db, job_id, status="EVALUATING", progress=progress)

        # 3단계: 총점 계산
        await update_job(db, job_id, status="SCORING", progress=70)
        score_data = calculate_total_score(results)
        await save_criteria_results(db, job_id, results, score_data)

        # 4단계: LLM 보고서 생성 (1회 호출)
        await update_job(db, job_id, status="GENERATING_REPORT", progress=80)
        report = await generate_report(score_data)

        # 5단계: S3 저장
        result_s3_key = f"reports/{job_id}/report.json"
        save_report_to_s3(s3_client, result_s3_key, report)

        await update_job(db, job_id, status="DONE", progress=100,
                         result_s3_key=result_s3_key)
        return result_s3_key

    except Exception as e:
        await update_job(db, job_id, status="FAILED", error_msg=str(e))
        raise
```

### evaluation/scoring.py — 총점 계산

```python
# worker/evaluation/scoring.py
from dataclasses import dataclass
from typing import List
from evaluation.criteria.base import CriteriaResult

@dataclass
class ScoreData:
    total_score: float      # 0.0 ~ 100.0
    grade: str              # A / B / C / D / F
    breakdown: List[CriteriaResult]

def calculate_total_score(results: List[CriteriaResult]) -> ScoreData:
    valid = [r for r in results if r.score is not None]
    total_weight = sum(r.weight for r in valid)

    weighted_score = (
        sum(r.score * r.weight for r in valid) / total_weight
        if total_weight > 0 else 0.0
    )

    return ScoreData(
        total_score=round(weighted_score, 2),
        grade=_to_grade(weighted_score),
        breakdown=results
    )

def _to_grade(score: float) -> str:
    if score >= 90: return "A"
    if score >= 75: return "B"
    if score >= 60: return "C"
    if score >= 40: return "D"
    return "F"
```

### report/generator.py — LLM 보고서 생성

```python
# worker/report/generator.py
import json
from openai import AsyncOpenAI
from evaluation.scoring import ScoreData

client = AsyncOpenAI()

async def generate_report(score_data: ScoreData) -> dict:
    """
    LLM에는 점수 요약만 전달한다. 원본 데이터셋은 절대 보내지 않는다.
    반환: 점수 + 보고서가 합쳐진 dict (S3에 저장됨)
    """
    breakdown_text = "\n".join([
        f"- {r.name}: {r.score}점 (가중치 {r.weight}) — {r.detail}"
        for r in score_data.breakdown
    ])

    prompt = f"""
당신은 데이터 품질 전문 컨설턴트입니다.
아래 평가 결과를 바탕으로 데이터를 처음 접하는 비전문가도 이해할 수 있는 보고서를 작성하세요.

[평가 결과]
종합 점수: {score_data.total_score}점 / 100점 (등급: {score_data.grade})
{breakdown_text}

[작성 규칙]
- 전문 용어를 쉬운 말로 풀어 설명한다.
- 각 항목이 AI 학습에 왜 중요한지 한 문장으로 설명한다.
- 점수가 낮은 항목은 구체적인 개선 방향을 제안한다.
- 전체 요약은 3문장 이내로 작성한다.

반드시 아래 JSON 형식으로만 응답하라. 다른 텍스트 없이 JSON만 출력하라.
{{
  "summary": "3문장 이내 전체 요약",
  "strengths": ["잘된 점1", "잘된 점2"],
  "weaknesses": ["부족한 점1", "부족한 점2"],
  "suggestions": ["개선 제안1", "개선 제안2"],
  "criteria_explanations": {{
    "기준명": "비전문가용 설명"
  }}
}}
"""

    response = await client.chat.completions.create(
        model="gpt-4o",
        messages=[{"role": "user", "content": prompt}],
        temperature=0.3,
        response_format={"type": "json_object"}
    )

    report_json = json.loads(response.choices[0].message.content)

    return {
        "score": {
            "total": score_data.total_score,
            "grade": score_data.grade,
            "breakdown": [
                {"name": r.name, "score": r.score, "weight": r.weight, "detail": r.detail}
                for r in score_data.breakdown
            ]
        },
        "report": report_json
    }
```

### global/messaging/consumer.py — Kafka Consumer

```python
# worker/global/messaging/consumer.py
import asyncio, json, os
from aiokafka import AIOKafkaConsumer
from evaluation.runner import run_pipeline
from global.db import get_db
from global.s3 import get_s3

KAFKA_BOOTSTRAP = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "kafka:9092")

async def start_consumer():
    consumer = AIOKafkaConsumer(
        "eval-request",
        bootstrap_servers=KAFKA_BOOTSTRAP,
        group_id="eval-workers",       # 같은 group_id → 파티션 자동 분산
        value_deserializer=lambda m: json.loads(m.decode("utf-8"))
    )
    await consumer.start()
    try:
        async for msg in consumer:
            job = msg.value
            asyncio.create_task(
                run_pipeline(job["job_id"], job["s3_key"], get_db(), get_s3())
            )
    finally:
        await consumer.stop()
```

---

## React 핵심 구현

### components/ProgressBar.jsx — SSE 연결

```jsx
import { useEffect, useState } from "react";

export default function ProgressBar({ jobId }) {
  const [status, setStatus] = useState({ progress: 0, status: "PENDING" });

  useEffect(() => {
    const source = new EventSource(`/api/jobs/${jobId}/stream`);

    source.onmessage = (e) => {
      const data = JSON.parse(e.data);
      setStatus(data);
      if (data.status === "DONE" || data.status === "FAILED") {
        source.close();
      }
    };

    source.onerror = () => source.close();
    return () => source.close();
  }, [jobId]);

  const statusLabel = {
    PENDING:           "대기 중",
    EVALUATING:        "데이터 평가 중",
    SCORING:           "점수 계산 중",
    GENERATING_REPORT: "보고서 생성 중",
    DONE:              "완료",
    FAILED:            "실패",
  }[status.status] ?? status.status;

  return (
    <div>
      <div>{statusLabel} — {status.progress}%</div>
      <div style={{ background: "#eee", borderRadius: 4, height: 8 }}>
        <div style={{
          width: `${status.progress}%`,
          background: status.status === "FAILED" ? "#e53e3e" : "#3182ce",
          height: "100%", borderRadius: 4, transition: "width 0.5s"
        }}/>
      </div>
    </div>
  );
}
```

---

## nginx.conf

```nginx
events {}
http {
  server {
    listen 80;

    location /api/ {
      proxy_pass http://backend:8080;
      proxy_set_header Host $host;

      # SSE를 위한 필수 설정
      proxy_buffering off;
      proxy_cache off;
      proxy_set_header X-Accel-Buffering no;
      chunked_transfer_encoding on;
    }

    location / {
      proxy_pass http://frontend:80;
    }
  }
}
```

---

## .env.example

```
MYSQL_ROOT_PASSWORD=changeme
OPENAI_API_KEY=sk-...
AWS_ACCESS_KEY_ID=...
AWS_SECRET_ACCESS_KEY=...
S3_BUCKET=ai-ready-platform
```

---

## 개발 순서 (권장)

```
1단계: 뼈대 (DB + API)
  - MySQL 스키마 생성 (Flyway V1__init.sql)
  - dataset 도메인: 업로드 → S3 저장 → DB 기록
  - job 도메인: 작업 제출 → DB 저장 → 상태 조회 API

2단계: AI 파이프라인 (FastAPI)
  - evaluation/criteria/mock_criteria.py 로 파이프라인 전체 구동 확인
  - report/generator.py LLM 보고서 생성 확인
  - 결과 MySQL + S3 저장 확인

3단계: 비동기 연결 (Kafka + SSE)
  - job/kafka/JobProducer → global/messaging/consumer 연결
  - eval-result 토픽으로 완료 신호 전송
  - job/kafka/JobResultConsumer → global/sse/SseService.push()
  - React ProgressBar SSE 동작 확인

4단계: 프론트엔드
  - 파일 업로드 → 작업 제출 → 진행률 → 결과 페이지

5단계: 인프라
  - Docker Compose 전체 구동 확인
  - GitHub Actions CI/CD 파이프라인
  - Prometheus + Grafana 대시보드
```

---

## 팀원 인터페이스 계약

팀원이 평가 로직을 구현할 때 지켜야 할 규칙:

```
1. worker/evaluation/criteria/base.py 의 EvalCriteria를 상속받는다.
2. evaluate(self, dataset: pd.DataFrame) -> CriteriaResult 를 구현한다.
3. score는 반드시 0.0 ~ 100.0 사이 float이다.
4. detail은 비전문가가 이해할 수 있는 한국어 한 줄 문자열이다.
5. 구현 완료 후 worker/evaluation/criteria/mock_criteria.py 의
   CRITERIA_LIST에 실제 구현체로 교체한다.
6. evaluation/runner.py, evaluation/scoring.py, report/generator.py 는 수정하지 않는다.
```
