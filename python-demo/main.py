import logging
import os
import random, time
from typing import Optional

import httpx
import uvicorn
from fastapi import FastAPI, Response
from opentelemetry.propagate import inject

from utils import PrometheusMiddleware, metrics, setting_otlp

APP_NAME = os.environ.get("APP_NAME", "app-a")
EXPOSE_PORT = os.environ.get("EXPOSE_PORT", 8000)
OTLP_GRPC_ENDPOIOT = os.environ.get(
    "OTLP_GRPC_ENDPOIOT", "http://tempo.monitoring:4317"
)

TARGET_ONE_HOST = os.environ.get("TARGET_ONE_HOST", "app-b")
TARGET_TWO_HOST = os.environ.get("TARGET_TWO_HOST", "app-c")

app=FastAPI()

# 设置 middleware
app.add_middleware(PrometheusMiddleware, app_name=APP_NAME)
app.add_route("/metrics", metrics)

setting_otlp(app, APP_NAME, OTLP_GRPC_ENDPOIOT)

class EndpointFilter(logging.Filter):
    def filter(self, record: logging.LogRecord) -> bool:
        return record.getMessage().find("GET /metrics") == -1

logging.getLogger("uvicorn.access").addFilter(EndpointFilter())

@app.get("/")
async def read_root():
    logging.error("Hello World")
    return {"Hello": "World"}

@app.get("/io_task")
async def io_task():
    time.sleep(1)
    logging.error("io task")
    return "IO Bound task finish!"

@app.get("/cpu_task")
async def cpu_task():
    for i in range(1000):
        _ = i*i*i
    logging.error("CPU TASK")
    return "CPU TASK FINISH"

@app.get("/chain")
async def chain(response: Response):
    headers = {}
    inject(headers)
    logging.critical(headers)

    async with httpx.AsyncClient() as client:
        await client.get("http://localhost:8000/", headers=headers)

    async with httpx.AsyncClient() as client:
        await client.get(f"http://{TARGET_ONE_HOST}:8000/io_task", headers=headers)

    async with httpx.AsyncClient() as client:
        await client.get(f"http://{TARGET_TWO_HOST}:8000/cpu_task", headers=headers)

    logging.info("Chain Finished")
    return {"ok": "true"}


if __name__ == "__main__":
    log_config = uvicorn.config.LOGGING_CONFIG
    log_config["formatters"]["access"][
        "fmt"
    ] = "%(asctime)s %(levelname)s [%(name)s] [%(filename)s:%(lineno)d] [trace_id=%(otelTraceID)s span_id=%(otelSpanID)s resource.service.name=%(otelServiceName)s] - %(message)s"

uvicorn.run(app, host='0.0.0.0', port=EXPOSE_PORT, log_config=log_config)