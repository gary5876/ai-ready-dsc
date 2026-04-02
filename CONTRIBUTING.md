# 팀원 기여 가이드

## 평가 기준(Criteria) 구현하기

FastAPI worker의 평가 기준은 `worker/evaluation/criteria/` 아래에 있습니다.  
팀원은 **이 디렉토리만** 수정합니다. 파이프라인 코드(`runner.py`, `scoring.py`, `report/generator.py`)는 건드리지 않습니다.

### 1단계 — `base.py` 계약 확인 (수정 금지)

```python
# worker/evaluation/criteria/base.py

@dataclass
class CriteriaResult:
    name: str       # 기준 이름 (한국어, 예: "완전성")
    score: float    # 0.0 ~ 100.0
    weight: float   # 0.0 ~ 1.0  (모든 기준의 weight 합 = 1.0)
    detail: str     # 비전문가용 한국어 한 줄 설명

class EvalCriteria(ABC):
    name: str
    weight: float

    @abstractmethod
    def evaluate(self, dataset: pd.DataFrame) -> CriteriaResult:
        pass
```

### 2단계 — 새 파일에 구현

```python
# worker/evaluation/criteria/my_criteria.py
import pandas as pd
from .base import EvalCriteria, CriteriaResult

class CompletenessEval(EvalCriteria):
    name = "완전성"
    weight = 0.3

    def evaluate(self, dataset: pd.DataFrame) -> CriteriaResult:
        total_cells = dataset.size
        missing_cells = dataset.isnull().sum().sum()
        missing_pct = round(missing_cells / total_cells * 100, 1)
        score = round(100 - missing_pct, 1)

        return CriteriaResult(
            name=self.name,
            score=score,
            weight=self.weight,
            detail=f"전체 데이터 중 {missing_pct}%에서 결측값이 발견되었습니다."
        )
```

### 3단계 — `mock_criteria.py`의 `CRITERIA_LIST` 교체

```python
# worker/evaluation/criteria/mock_criteria.py 하단

from .my_criteria import CompletenessEval  # 추가

CRITERIA_LIST = [
    CompletenessEval(),        # Mock → 실제 구현으로 교체
    MockConsistencyEval(),
    MockUniquenessEval(),
    MockAccuracyEval(),
]
```

### 규칙 요약

| 규칙 | 내용 |
|------|------|
| `score` 범위 | 반드시 `0.0 ~ 100.0` |
| `weight` 합 | 모든 기준의 weight 합이 반드시 `1.0` |
| `detail` 언어 | 한국어, 비전문가가 이해할 수 있는 수준 |
| 예외 처리 | `evaluate()` 내부에서 예외가 발생하면 runner가 자동으로 score=0 처리하므로 직접 try/catch 불필요 |
| 수정 금지 파일 | `base.py`, `runner.py`, `scoring.py`, `report/generator.py` |

---

## PR 체크리스트

- [ ] `weight` 합이 1.0인지 확인
- [ ] `score`가 항상 0.0~100.0 범위 내에 있는지 확인
- [ ] `detail`이 한국어 한 줄인지 확인
- [ ] Mock 파일의 `CRITERIA_LIST`가 업데이트 되었는지 확인
- [ ] `docker compose up --build` 후 정상 동작 확인

## 커밋 컨벤션

```
feat:   새 기능
fix:    버그 수정
docs:   문서 수정
test:   테스트 추가/수정
chore:  빌드·설정 변경
```

예시: `feat: 완전성 평가 기준 실제 구현으로 교체`
