import asyncio
import json
import os
from aiokafka import AIOKafkaConsumer
from evaluation.runner import run_pipeline
from global_.db import get_db
from global_.s3 import get_s3

KAFKA_BOOTSTRAP = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "kafka:9092")

async def start_consumer():
    consumer = AIOKafkaConsumer(
        "eval-request",
        bootstrap_servers=KAFKA_BOOTSTRAP,
        group_id="eval-workers",
        value_deserializer=lambda m: json.loads(m.decode("utf-8"))
    )
    await consumer.start()
    try:
        async for msg in consumer:
            job = msg.value
            asyncio.create_task(
                run_pipeline(job["job_id"], job["s3_key"], get_db(), get_s3())
            )
    finally:
        await consumer.stop()
