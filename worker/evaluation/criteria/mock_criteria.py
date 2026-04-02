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
