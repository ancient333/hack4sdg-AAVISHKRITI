from pathlib import Path
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import joblib
import numpy as np

ROOT = Path(__file__).resolve().parent
MODEL_PATH = ROOT / "models" / "isl_classifier.joblib"
app = FastAPI(title="Signal Sign Recognition API")
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])
model = None

class PredictionInput(BaseModel):
    landmarks: list[float]

@app.on_event("startup")
def load_model():
    global model
    if not MODEL_PATH.exists():
        raise RuntimeError(f"Trained model not found: {MODEL_PATH}. Copy your own isl_classifier.joblib into the models folder.")
    model = joblib.load(MODEL_PATH)

@app.get("/")
def home():
    return {"status": "recognition API running", "model_loaded": model is not None}

@app.post("/predict")
def predict(data: PredictionInput):
    if len(data.landmarks) != 126:
        raise HTTPException(status_code=422, detail=f"Expected 126 features, received {len(data.landmarks)}")
    features = np.asarray(data.landmarks, dtype=np.float32).reshape(1, -1)
    prediction = model.predict(features)[0]
    confidence = float(np.max(model.predict_proba(features)[0])) if hasattr(model, "predict_proba") else 0.0
    return {"text": str(prediction), "confidence": round(confidence, 3)}
