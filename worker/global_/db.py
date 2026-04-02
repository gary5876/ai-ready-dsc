import os
from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession
from sqlalchemy.orm import sessionmaker

MYSQL_URL = os.getenv("MYSQL_URL", "mysql+asyncmy://root:changeme@localhost:3306/aiready")

engine = create_async_engine(MYSQL_URL, echo=False)
AsyncSessionLocal = sessionmaker(engine, class_=AsyncSession, expire_on_commit=False)

def get_db() -> AsyncSession:
    return AsyncSessionLocal()
