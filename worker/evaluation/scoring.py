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
