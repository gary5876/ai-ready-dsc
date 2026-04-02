import io
import os
import json
import pandas as pd
from evaluation.criteria.mock_criteria import CRITERIA_LIST
from evaluation.criteria.base import CriteriaResult
from evaluation.scoring import calculate_total_score
from report.generator import generate_report
from global_.messaging.producer import send_result

S3_BUCKET = os.getenv("S3_BUCKET", "ai-ready-platform")


def load_dataset_from_s3(s3_client, s3_key: str) -> pd.DataFrame:
    obj = s3_client.get_object(Bucket=S3_BUCKET, Key=s3_key)
    return pd.read_csv(io.BytesIO(obj["Body"].read()))


def save_report_to_s3(s3_client, s3_key: str, report: dict):
    s3_client.put_object(
        Bucket=S3_BUCKET,
        Key=s3_key,
        Body=json.dumps(report, ensure_ascii=False).encode("utf-8"),
        ContentType="application/json"
    )


async def update_job(db, job_id: int, status: str, progress: int = None,
                     result_s3_key: str = None, error_msg: str = None):
    from sqlalchemy import text
    async with db as session:
        set_clauses = ["status = :status", "updated_at = NOW()"]
        params = {"status": status, "job_id": job_id}
        if progress is not None:
            set_clauses.append("progress = :progress")
            params["progress"] = progress
        if result_s3_key is not None:
            set_clauses.append("result_s3_key = :result_s3_key")
            params["result_s3_key"] = result_s3_key
        if error_msg is not None:
            set_clauses.append("error_msg = :error_msg")
            params["error_msg"] = error_msg
        await session.execute(
            text(f"UPDATE eval_jobs SET {', '.join(set_clauses)} WHERE id = :job_id"),
            params
        )
        await session.commit()


async def save_criteria_results(db, job_id: int, results, score_data):
    from sqlalchemy import text
    async with db as session:
        for r in results:
            await session.execute(
                text("INSERT INTO eval_results (job_id, criteria_name, score, weight, detail) "
                     "VALUES (:job_id, :name, :score, :weight, :detail)"),
                {"job_id": job_id, "name": r.name, "score": r.score,
                 "weight": r.weight, "detail": r.detail}
            )
        await session.commit()


async def run_pipeline(job_id: int, s3_key: str, db, s3_client) -> str:
    """
    전체 평가 파이프라인 실행.
    반환값: 보고서가 저장된 S3 key
    """
    try:
        # 1단계: 데이터 로드
        await update_job(db, job_id, status="EVALUATING", progress=5)
        dataset = load_dataset_from_s3(s3_client, s3_key)

        # 2단계: 평가기준별 실행
        results = []
        for i, criteria in enumerate(CRITERIA_LIST):
            try:
                result = criteria.evaluate(dataset)
                results.append(result)
            except Exception as e:
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
        await send_result(job_id, "DONE", result_s3_key=result_s3_key)
        return result_s3_key

    except Exception as e:
        await update_job(db, job_id, status="FAILED", error_msg=str(e))
        await send_result(job_id, "FAILED", error_msg=str(e))
        raise
