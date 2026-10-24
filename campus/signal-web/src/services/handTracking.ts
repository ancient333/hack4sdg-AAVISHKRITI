import { FilesetResolver, HandLandmarker, type HandLandmarkerResult } from '@mediapipe/tasks-vision';

export async function createHandTracker() {
  const files = await FilesetResolver.forVisionTasks('/mediapipe');
  return HandLandmarker.createFromOptions(files, {
    baseOptions: { modelAssetPath: '/models/hand_landmarker.task', delegate: 'CPU' },
    runningMode: 'VIDEO', numHands: 2,
    minHandDetectionConfidence: 0.5,
    minHandPresenceConfidence: 0.5,
    minTrackingConfidence: 0.5,
  });
}

export function drawHands(canvas: HTMLCanvasElement, result: HandLandmarkerResult) {
  const context = canvas.getContext('2d');
  if (!context) return;
  context.clearRect(0, 0, canvas.width, canvas.height);
  for (const points of result.landmarks) {
    context.strokeStyle = '#34ffb4'; context.lineWidth = 3;
    for (const edge of HandLandmarker.HAND_CONNECTIONS) {
      context.beginPath();
      context.moveTo(points[edge.start].x * canvas.width, points[edge.start].y * canvas.height);
      context.lineTo(points[edge.end].x * canvas.width, points[edge.end].y * canvas.height);
      context.stroke();
    }
    context.fillStyle = '#ffdc51';
    for (const point of points) {
      context.beginPath();
      context.arc(point.x * canvas.width, point.y * canvas.height, 4, 0, 2 * Math.PI);
      context.fill();
    }
  }
}

export interface SignClassifier {
  predict(frame: HandLandmarkerResult, timestamp: number): { text: string; confidence: number } | null;
  reset(): void;
}

type Prediction = { text: string; confidence: number };
let latestPrediction: Prediction | null = null;
let lastRequestTime = 0;
let requestPending = false;
let requestVersion = 0;

export const signClassifier: SignClassifier = {
  predict(frame, timestamp) {
    if (frame.landmarks.length === 0) {
      latestPrediction = null;
      return null;
    }
    if (!requestPending && timestamp - lastRequestTime >= 250) {
      const features: number[] = [];
      for (let handIndex = 0; handIndex < 2; handIndex++) {
        const hand = frame.landmarks[handIndex];
        if (hand) {
          for (const point of hand) features.push(point.x, point.y, point.z);
        } else {
          features.push(...Array(63).fill(0));
        }
      }
      const currentVersion = requestVersion;
      lastRequestTime = timestamp;
      requestPending = true;
      fetch('/recognition/predict', {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ landmarks: features }),
      })
        .then(async response => {
          if (!response.ok) throw new Error(`Recognition API error: ${response.status}`);
          return response.json() as Promise<Prediction>;
        })
        .then(data => {
          if (currentVersion !== requestVersion) return;
          latestPrediction = typeof data.text === 'string' && typeof data.confidence === 'number' && data.confidence >= 0.6
            ? { text: data.text, confidence: data.confidence } : null;
        })
        .catch(error => {
          if (currentVersion === requestVersion) {
            latestPrediction = null;
            console.error('Sign recognition failed. Check that recognition_api.py is running on port 8002 and the trained model is installed.', error);
          }
        })
        .finally(() => {
          if (currentVersion === requestVersion) requestPending = false;
        });
    }
    return latestPrediction;
  },
  reset() {
    requestVersion++;
    latestPrediction = null;
    lastRequestTime = 0;
    requestPending = false;
  },
};
