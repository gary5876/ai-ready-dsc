import json
import os
from aiokafka import AIOKafkaProducer

KAFKA_BOOTSTRAP = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "kafka:9092")

async def send_result(job_id: int, status: str, result_s3_key: str = None, error_msg: str = None):
    producer = AIOKafkaProducer(
        bootstrap_servers=KAFKA_BOOTSTRAP,
        value_serializer=lambda v: json.dumps(v).encode("utf-8")
    )
    await producer.start()
    try:
        await producer.send("eval-result", key=str(job_id).encode(), value={
            "job_id": job_id,
            "status": status,
            "result_s3_key": result_s3_key,
            "error_msg": error_msg,
        })
    finally:
        await producer.stop()
