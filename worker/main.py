import asyncio
from global_.messaging.consumer import start_consumer

if __name__ == "__main__":
    asyncio.run(start_consumer())
