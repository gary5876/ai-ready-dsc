# Backend — Spring Boot 3

## 개요

Java 21 + Spring Boot 3 기반 메인 백엔드입니다.  
파일 업로드, 작업 관리, Kafka 연동, SSE 실시간 스트리밍을 담당합니다.

## 도메인 구조

```
src/main/java/com/aiready/
├── global/
│   ├── config/        KafkaConfig, SecurityConfig, S3Config
│   ├── exception/     GlobalExceptionHandler
│   └── sse/           SseService (SSE emitter 관리)
│
├── dataset/           데이터셋 업로드 및 목록 조회
├── job/               평가 작업 생성·상태 조회·SSE 스트리밍
│   └── kafka/         JobProducer, JobResultConsumer
└── result/            평가 결과 조회
```

## API 엔드포인트

### 데이터셋

| 메서드 | 경로 | 설명 |
|--------|------|------|
| `POST` | `/api/datasets/upload` | CSV 업로드 → S3 저장 |
| `GET`  | `/api/datasets?userId={id}` | 내 데이터셋 목록 |

**업로드 요청 (multipart/form-data)**
```
file   : CSV 파일
name   : 데이터셋 이름
userId : 사용자 ID (임시, 추후 인증으로 교체)
```

### 작업

| 메서드 | 경로 | 설명 |
|--------|------|------|
| `POST` | `/api/jobs` | 평가 작업 제출 → jobId 즉시 반환 |
| `GET`  | `/api/jobs?userId={id}` | 내 작업 목록 |
| `GET`  | `/api/jobs/{jobId}` | 작업 상태 조회 |
| `GET`  | `/api/jobs/{jobId}/stream` | SSE 실시간 진행률 (text/event-stream) |
| `GET`  | `/api/jobs/{jobId}/result` | 완료된 평가 결과 조회 |

**작업 상태값**

```
PENDING → EVALUATING → SCORING → GENERATING_REPORT → DONE
                                                    ↘ FAILED
```

**작업 제출 요청 (JSON)**
```json
{ "datasetId": 1, "userId": 1 }
```

**SSE 이벤트 형식**
```json
{ "jobId": 1, "status": "EVALUATING", "resultS3Key": null }
```

## 로컬 실행

```bash
# MySQL, Kafka가 실행 중이어야 합니다 (docker compose로 인프라만 띄우기)
docker compose up -d mysql kafka zookeeper

cd backend
./gradlew bootRun
```

환경 변수는 `src/main/resources/application.yml`의 기본값을 참고하거나 직접 설정합니다.

```bash
export SPRING_DATASOURCE_URL=jdbc:mysql://localhost:3306/aiready
export SPRING_DATASOURCE_PASSWORD=changeme
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
export AWS_ACCESS_KEY_ID=...
export AWS_SECRET_ACCESS_KEY=...
export S3_BUCKET=ai-ready-platform
```

## 빌드

```bash
./gradlew build        # 전체 빌드 + 테스트
./gradlew bootJar      # 실행 가능한 JAR 생성
```

## DB 마이그레이션

Flyway를 사용합니다. 서버 시작 시 `src/main/resources/db/migration/` 아래의 SQL이 자동 적용됩니다.

새 마이그레이션 추가 시 파일명 규칙: `V{번호}__{설명}.sql` (예: `V2__add_user_table.sql`)

## Kafka 토픽

| 토픽 | 방향 | 설명 |
|------|------|------|
| `eval-request` | 발행 | FastAPI로 평가 요청 전송 |
| `eval-result`  | 수신 | FastAPI로부터 완료/실패 결과 수신 |
