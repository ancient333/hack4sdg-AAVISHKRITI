import { cpSync, existsSync, mkdirSync } from 'node:fs';
import { resolve } from 'node:path';

const runtime = resolve('node_modules/@mediapipe/tasks-vision/wasm');
if (!existsSync(runtime)) throw new Error('MediaPipe is missing. Run npm install first.');
if (!existsSync('public/models/hand_landmarker.task')) throw new Error('Hand model missing: restore public/models/hand_landmarker.task (see THIRD_PARTY.md).');
mkdirSync('public/mediapipe', { recursive: true });
cpSync(runtime, 'public/mediapipe', { recursive: true });
console.log('Local MediaPipe runtime synchronized with the installed package.');
