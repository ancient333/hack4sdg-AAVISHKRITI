import { useEffect, useRef, useState } from 'react';
import { Camera as CameraIcon, VideoOff } from 'lucide-react';
import { createHandTracker, drawHands, signClassifier } from '../services/handTracking';
import type { HandLandmarker } from '@mediapipe/tasks-vision';
export default function Camera({ onRecognized }: { onRecognized: (text: string) => void }) {
  const video = useRef<HTMLVideoElement>(null);
  const stream = useRef<MediaStream | null>(null);
  const request = useRef(0);
  const [active, setActive] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const overlay = useRef<HTMLCanvasElement>(null);
  const tracker = useRef<HandLandmarker | null>(null);
  const recognized = useRef(onRecognized);
  recognized.current = onRecognized;
  const [tracking, setTracking] = useState('Loading hand-landmark model…');
  const [modelReady, setModelReady] = useState(false);
  const [modelError, setModelError] = useState('');
  const [retry, setRetry] = useState(0);
  useEffect(() => {
    let disposed = false;
    setModelReady(false); setModelError(''); setTracking('Loading hand-landmark model…');
    createHandTracker().then(model => {
      if (disposed) { model.close(); return; }
      tracker.current = model; setModelReady(true); setTracking('Hand-landmark model ready. Start the camera to track hands.');
    }).catch(() => { if (!disposed) { setModelError('Hand tracking could not load. Check the local model/runtime files and retry. The camera preview can still run.'); setTracking('Hand tracking unavailable.'); } });
    return () => { disposed = true; tracker.current?.close(); tracker.current = null; };
  }, [retry]);
  useEffect(() => {
    if (!active || !modelReady) return;
    let frame = 0, lastTime = -1, lastRun = 0, count = 0, lastWord = '';
    signClassifier?.reset();
    function process(timestamp: number) {
      const source = video.current, canvas = overlay.current, model = tracker.current;
      if (!source || !canvas || !model) return;
      if (source.readyState >= 2 && timestamp - lastRun >= 100 && source.currentTime !== lastTime) {
        lastTime = source.currentTime; lastRun = timestamp;
        try {
          if (canvas.width !== source.videoWidth || canvas.height !== source.videoHeight) { canvas.width = source.videoWidth; canvas.height = source.videoHeight; }
          const result = model.detectForVideo(source, timestamp);
          drawHands(canvas, result); count++;
          setTracking(`Processing live frames · ${count} frames · ${result.landmarks.length} hand(s) detected`);
          const prediction = signClassifier?.predict(result, timestamp);
          if (prediction && prediction.confidence >= 0.8 && prediction.text.trim() && prediction.text !== lastWord) {
            lastWord = prediction.text; recognized.current(prediction.text);
          }
        } catch {
          setModelError('Hand tracking stopped processing frames. Stop the camera and retry the tracker.');
          setTracking('Tracking failed; camera preview remains available.'); return;
        }
      }
      frame = requestAnimationFrame(process);
    }
    frame = requestAnimationFrame(process);
    return () => { cancelAnimationFrame(frame); const canvas = overlay.current; canvas?.getContext('2d')?.clearRect(0, 0, canvas.width, canvas.height); signClassifier?.reset(); };
  }, [active, modelReady, retry]);

  function release() {
    stream.current?.getTracks().forEach(track => { track.onended = null; track.stop(); });
    stream.current = null;
    if (video.current) video.current.srcObject = null;
  }
  function stop() { request.current++; release(); setActive(false); setBusy(false); }
  useEffect(() => () => { request.current++; release(); }, []);

  async function start() {
    const current = ++request.current;
    setError(''); setBusy(true);
    try {
      if (!navigator.mediaDevices?.getUserMedia) throw new Error('Camera requires HTTPS or localhost and a browser with camera support.');
      const media = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'user', width: { ideal: 640 }, height: { ideal: 480 } }, audio: false });
      if (current !== request.current) { media.getTracks().forEach(track => track.stop()); return; }
      stream.current = media;
      media.getVideoTracks().forEach(track => { track.onended = () => { stop(); setError('Camera access ended. Reconnect your camera or check permissions, then try again.'); }; });
      video.current!.srcObject = media;
      await video.current!.play();
      if (current === request.current) setActive(true);
    } catch (e) {
      if (current !== request.current) return;
      release();
      const name = e instanceof DOMException ? e.name : '';
      setError(name === 'NotAllowedError' ? 'Camera permission denied. Allow camera access in your browser settings, then try again.'
        : name === 'NotFoundError' ? 'No camera found. Connect a camera and try again.'
        : name === 'NotReadableError' ? 'The camera is busy or unavailable. Close other camera apps and try again.'
        : e instanceof Error ? e.message : 'Camera unavailable. Please try again.');
    } finally { if (current === request.current) setBusy(false); }
  }
  return <>
    <div className="camera-stage">
      <video ref={video} muted playsInline className={active ? '' : 'hidden'} aria-label="Live camera preview"/>
      <canvas ref={overlay} className={`hand-overlay ${active ? '' : 'hidden'}`} aria-label="Detected hand skeleton overlay"/>
      {!active && <div className="camera-empty"><span className="large-icon"><CameraIcon size={32}/></span><h3>Your hands. Your voice.</h3><p>Position your face and hands in the frame.<br/>Your camera stays on this device.</p><button className="primary" onClick={start} disabled={busy}><CameraIcon size={17}/>{busy ? 'Waiting for camera permission…' : 'Start camera'}</button></div>}
      {(active || busy) && <button className="stop-camera" onClick={stop}><VideoOff size={16}/>{busy ? 'Cancel camera request' : 'Stop camera'}</button>}
      <span className="stage-label">{active ? 'Camera on · Local preview' : 'Camera off'}</span>
    </div>
    <p className="recognition-status" role="status"><span className="status-dot"/>{active ? 'Camera active. Hand tracking runs below; a trained sign-language classifier is not installed, so words cannot yet be identified.' : busy ? 'Waiting for camera permission. Hand tracking and sign classification status will appear here.' : 'Camera is not started.'}</p>
    <p className="tracking-status" role="status">{!active && modelReady ? 'Hand-landmark model ready. Start the camera to track hands.' : tracking}</p>
    <p className="input-hint">{signClassifier ? 'Sign classifier connected.' : 'Hand tracking detects finger and hand positions. It cannot identify words without a trained sign-language classifier.'}</p>
    {modelError && <div className="error" role="alert">{modelError}<button className="secondary" onClick={() => setRetry(v => v + 1)}>Retry hand tracking</button></div>}
    {error && <p className="error" role="alert">{error}</p>}
  </>;
}
