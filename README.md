# AI-Ready Data 평가 플랫폼

CSV 데이터셋을 업로드하면 여러 품질 기준으로 점수를 산출하고, LLM이 비전문가도 이해할 수 있는 보고서를 자동 생성하는 웹 플랫폼입니다.

## 아키텍처

```
사용자 브라우저
     │
     ▼
  [Nginx :80]
     ├── /api/*  ──▶  [Spring Boot :8080]
     │                     │  Kafka eval-request
     │                     │──────────────────▶  [FastAPI Worker × 4]
     │                     │                          │
     │                     │  Kafka eval-result        │ S3, MySQL
     │                     │◀──────────────────        │
     │                SSE push to browser ◀────────────┘
     └── /*      ──▶  [React (Nginx :80)]
```

| 서비스 | 역할 | 포트 |
|--------|------|------|
| React  | 파일 업로드, 진행률, 결과 화면 | 3000 (dev) / 80 |
| Spring Boot | REST API, Kafka 발행/수신, SSE 스트리밍 | 8080 |
| FastAPI | 평가 파이프라인, LLM 보고서 생성 | — (내부) |
| Kafka | 비동기 작업 큐 (partition 4) | 9092 |
| MySQL 8 | 데이터셋/작업/결과 저장 | 3306 |
| Amazon S3 | 원본 CSV, 보고서 JSON 저장 | — |
| Prometheus + Grafana | 메트릭 수집·시각화 | 9090 / 3001 |

## 디렉토리 구조

```
ai-ready-dsc/
├── docker-compose.yml
├── nginx.conf
├── prometheus.yml
├── .env.example
│
├── backend/          # Spring Boot 3 (Java 21)
├── worker/           # FastAPI (Python 3.11)
└── frontend/         # React 18 (Vite)
```

## 빠른 시작

### 사전 요구사항

- Docker Desktop 4.x 이상
- AWS S3 버킷 (또는 LocalStack)
- OpenAI API 키

### 1. 환경 변수 설정

```bash
cp .env.example .env
# .env 파일을 열어 아래 값을 채운다
# MYSQL_ROOT_PASSWORD, OPENAI_API_KEY, AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY, S3_BUCKET
```

### 2. 전체 서비스 실행

```bash
docker compose up --build
```

| URL | 설명 |
|-----|------|
| http://localhost | 웹 UI |
| http://localhost:8080/actuator/health | 백엔드 헬스체크 |
| http://localhost:3001 | Grafana 대시보드 |

### 3. 개발 서버 (로컬)

백엔드, 워커, 프론트엔드를 각각의 README를 참고해 개별 실행할 수 있습니다.

- [backend/README.md](backend/README.md)
- [worker/README.md](worker/README.md)
- [frontend/README.md](frontend/README.md)

## 주요 흐름

```
1. 사용자가 CSV 파일 업로드
   → Spring Boot가 S3에 저장, datasets 테이블 기록

2. 사용자가 평가 작업 제출
   → Spring Boot가 eval_jobs 생성 후 Kafka eval-request 발행
   → jobId 즉시 반환 (비동기)

3. FastAPI Worker가 메시지 수신
   → 데이터 로드 → 기준별 평가 → 총점 계산 → LLM 보고서 생성
   → 결과를 S3 + MySQL에 저장
   → Kafka eval-result 발행

4. Spring Boot가 eval-result 수신
   → eval_jobs 상태 업데이트
   → SSE로 브라우저에 실시간 push

5. 브라우저 ProgressBar가 상태 표시 → 완료 시 결과 페이지 표시
```

## 팀원 역할 분담

| 담당 | 영역 |
|------|------|
| 백엔드 | `backend/` — Spring Boot API, Kafka 연동 |
| AI 워커 | `worker/evaluation/criteria/` — 평가 기준 구현 |
| 프론트엔드 | `frontend/` — UI/UX |

평가 기준 구현 방법은 [CONTRIBUTING.md](CONTRIBUTING.md)를 참고하세요.

## 브랜치 전략

```
main      ← 배포 브랜치 (PR만 허용)
develop   ← 통합 브랜치
feat/*    ← 기능 개발
fix/*     ← 버그 수정
```
