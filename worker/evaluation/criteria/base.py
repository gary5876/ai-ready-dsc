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

    절대 수정 금지 (인터페이스 계약)
    """
    name: str
    weight: float

    @abstractmethod
    def evaluate(self, dataset) -> CriteriaResult:
        pass
