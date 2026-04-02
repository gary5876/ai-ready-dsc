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
