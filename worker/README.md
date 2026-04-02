# Worker — FastAPI (Python 3.11)

## 개요

Kafka `eval-request` 토픽을 구독하여 평가 파이프라인을 실행하고, 결과를 `eval-result`로 발행합니다.  
인스턴스를 4개 띄워 Kafka Partition과 1:1 매칭합니다.

## 디렉토리 구조

```
worker/
├── main.py                       진입점 (consumer 시작)
├── global_/
│   ├── db.py                     SQLAlchemy async 세션
│   ├── s3.py                     boto3 S3 클라이언트
│   └── messaging/
│       ├── consumer.py           Kafka consumer (eval-request 수신)
│       └── producer.py           Kafka producer (eval-result 발행)
│
├── evaluation/
│   ├── runner.py                 파이프라인 오케스트레이터 (수정 금지)
│   ├── scoring.py                총점·등급 계산 (수정 금지)
│   └── criteria/
│       ├── base.py               EvalCriteria 인터페이스 (수정 금지)
│       └── mock_criteria.py      Mock 구현 + CRITERIA_LIST (팀원이 교체)
│
└── report/
    └── generator.py              OpenAI gpt-4o 보고서 생성 (수정 금지)
```

## 평가 파이프라인

```
Kafka eval-request 수신
        │
        ▼
① 데이터 로드 (S3 → pandas DataFrame)         progress: 5%
        │
        ▼
② 기준별 evaluate() 순차 실행                  progress: 5~65%
        │
        ▼
③ 총점·등급 계산 + MySQL eval_results 저장     progress: 70%
        │
        ▼
④ LLM 보고서 생성 (gpt-4o, 1회 호출)          progress: 80%
        │
        ▼
⑤ 보고서 JSON → S3 저장 + DB 상태 DONE        progress: 100%
        │
        ▼
Kafka eval-result 발행
```

## 평가 기준 구현 (팀원 작업)

자세한 내용은 프로젝트 루트의 [CONTRIBUTING.md](../CONTRIBUTING.md)를 참고하세요.

핵심만 요약하면:
1. `evaluation/criteria/base.py`의 `EvalCriteria`를 상속
2. `evaluate(self, dataset: pd.DataFrame) -> CriteriaResult` 구현
3. `mock_criteria.py`의 `CRITERIA_LIST`에 실제 구현체로 교체

## 로컬 실행

```bash
cd worker

# 가상환경 생성
python -m venv .venv
source .venv/bin/activate      # Windows: .venv\Scripts\activate

# 의존성 설치
pip install -r requirements.txt

# 환경 변수 설정
export KAFKA_BOOTSTRAP_SERVERS=localhost:9092
export MYSQL_URL=mysql+asyncmy://root:changeme@localhost:3306/aiready
export OPENAI_API_KEY=sk-...
export AWS_ACCESS_KEY_ID=...
export AWS_SECRET_ACCESS_KEY=...
export S3_BUCKET=ai-ready-platform

# 실행
python main.py
```

인프라는 docker compose로 먼저 띄웁니다:
```bash
docker compose up -d mysql kafka zookeeper
```

## 환경 변수

| 변수 | 설명 | 기본값 |
|------|------|--------|
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka 주소 | `kafka:9092` |
| `MYSQL_URL` | DB 연결 문자열 | `mysql+asyncmy://root:changeme@localhost:3306/aiready` |
| `OPENAI_API_KEY` | OpenAI API 키 | — |
| `AWS_ACCESS_KEY_ID` | AWS 액세스 키 | — |
| `AWS_SECRET_ACCESS_KEY` | AWS 시크릿 키 | — |
| `S3_BUCKET` | S3 버킷 이름 | `ai-ready-platform` |

## LLM 호출 방식

- 원본 데이터셋은 절대 LLM에 전달하지 않습니다.
- 점수 요약(항목명, 점수, 가중치, 한 줄 설명)만 프롬프트에 포함합니다.
- `response_format: json_object`로 JSON 직접 파싱합니다.

## 보고서 출력 형식 (S3 저장)

```json
{
  "score": {
    "total": 82.5,
    "grade": "B",
    "breakdown": [
      { "name": "완전성", "score": 88.0, "weight": 0.3, "detail": "..." },
      ...
    ]
  },
  "report": {
    "summary": "...",
    "strengths": ["..."],
    "weaknesses": ["..."],
    "suggestions": ["..."],
    "criteria_explanations": { "완전성": "..." }
  }
}
```
