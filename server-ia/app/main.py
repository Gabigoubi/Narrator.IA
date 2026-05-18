from fastapi import FastAPI
from fastapi.responses import StreamingResponse
from pydantic import BaseModel

app = FastAPI()

class PlayerTelemetry(BaseModel):
    event_type: str
    context_details: str

@app.get("/health")
def check_health():
    return {"status": "healthy"}

@app.post("/narrate")
def generate_narration(telemetry: PlayerTelemetry):
    pass