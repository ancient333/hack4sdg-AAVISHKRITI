# Signal sign-recognition module for CampusLens

This ZIP contains the Signal web app with its browser-side sign classifier connected to a local Python recognition API. It is **not a merged CampusLens Android project/APK**; the CampusLens Android source ZIP was not available in this working session, so no Android files have been changed.

## Run on your Windows computer

1. Extract this ZIP.
2. Copy your own trained model file `isl_classifier.joblib` into a new folder named `models` beside `recognition_api.py`, so the path is `models/isl_classifier.joblib`. The model is not included in this ZIP because it was not present in the uploaded project archive.
3. In the extracted project folder, open Terminal and run:

   ```powershell
   py -m pip install -r recognition-requirements.txt
   py -m uvicorn recognition_api:app --host 127.0.0.1 --port 8002
   ```

4. Keep that terminal running. Open a second terminal in the same folder and run:

   ```powershell
   npm install
   npm run dev
   ```

5. Open the Vite URL printed in the terminal (normally `http://localhost:5173`). The app sends prediction requests through `/recognition/predict`, which Vite proxies to the Python API on port 8002.
6. Verify API status at `http://127.0.0.1:8002/` and interactive docs at `http://127.0.0.1:8002/docs`.

## Important limitations

- The included app uses a model trained on the user's A/B/C samples only. It will only attempt to classify the labels in that model; it is not full ISL sentence recognition.
- The training CSV/model must use the same 126-feature order as the browser (two hands, 21 landmarks each, x/y/z). Different normalization, left/right ordering, or missing-hand conventions can make predictions unreliable.
- Confidence filtering is set to 0.60. Real-world accuracy needs testing with fresh samples not used for training.
- The Vite proxy works for local development. It does not automatically configure a deployed website or a native Android app. For CampusLens Android integration, the Android app must call a reachable deployed HTTPS API (or an API hosted on the same local network with appropriate network permissions); `127.0.0.1` on Android refers to the phone itself. A merged APK requires the actual CampusLens Android source ZIP.

## CampusLens handoff

The recognition endpoint contract is `POST /predict` with JSON `{ "landmarks": [126 numeric values] }`, returning `{ "text": "A", "confidence": 0.91 }`. The Android app must extract the same 126 features and send them to this endpoint, then display/speak the returned text in Deaf Mode. Do not treat this A/B/C prototype as full sign-language translation.
