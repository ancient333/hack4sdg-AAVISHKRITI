import { useEffect, useRef, useState } from 'react';
import * as THREE from 'three';

export default function Avatar({ demo, replay }: { demo: string; replay: number }) {
  const host = useRef<HTMLDivElement>(null);
  const gesture = useRef({ name: demo, started: performance.now() });
  const [state, setState] = useState('Loading 3D preview…');
  const [attempt, setAttempt] = useState(0);
  useEffect(() => { gesture.current = { name: demo, started: performance.now() }; }, [demo, replay]);
  useEffect(() => {
    const target = host.current!;
    setState('Loading 3D preview…');
    let renderer: THREE.WebGLRenderer | undefined;
    let frame = 0;
    let resize: ResizeObserver | undefined;
    let stopped = false;
    let redraw = () => {};
    const contextLost = (event: Event) => {
      event.preventDefault(); stopped = true; cancelAnimationFrame(frame);
      setState('The 3D graphics connection was lost. Retry the preview; messages still work.');
    };
    const visibilityChanged = () => { if (!document.hidden) redraw(); };
    const scene = new THREE.Scene();
    try {
      renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
      renderer.setPixelRatio(Math.min(devicePixelRatio, 2));
      target.appendChild(renderer.domElement);
      renderer.domElement.addEventListener('webglcontextlost', contextLost);
      const camera = new THREE.PerspectiveCamera(34, 1, 0.1, 100);
      camera.position.set(0, 1.5, 6.5); camera.lookAt(0, 1.25, 0);
      scene.add(new THREE.HemisphereLight(0xffffff, 0x6d8075, 3));
      const light = new THREE.DirectionalLight(0xffffff, 3); light.position.set(3, 5, 4); scene.add(light);
      const skin = new THREE.MeshStandardMaterial({ color: '#cc9877', roughness: 0.8 });
      const shirt = new THREE.MeshStandardMaterial({ color: '#38685e', roughness: 0.9 });
      const dark = new THREE.MeshStandardMaterial({ color: '#253d39' });
      function ellipsoid(x: number, y: number, z: number, sx: number, sy: number, sz: number, material: THREE.Material, parent: THREE.Object3D = scene) {
        const mesh = new THREE.Mesh(new THREE.SphereGeometry(1, 28, 20), material);
        mesh.position.set(x,y,z); mesh.scale.set(sx,sy,sz); parent.add(mesh); return mesh;
      }
      ellipsoid(0,0.75,0,0.61,0.83,0.3,shirt);
      ellipsoid(0,1.58,0,0.15,0.25,0.15,skin);
      ellipsoid(0,2.03,0,0.35,0.46,0.31,skin);
      ellipsoid(0,2.28,-0.05,0.36,0.25,0.30,dark);
      for (const side of [-1,1]) {
        ellipsoid(side*0.12,2.07,0.29,0.026,0.034,0.016,dark);
        ellipsoid(side*0.35,2,0,0.06,0.1,0.06,skin);
      }
      ellipsoid(0,1.88,0.294,0.08,0.014,0.014,dark);
      const arms: { shoulder: THREE.Group; elbow: THREE.Group; wrist: THREE.Group; fingers: THREE.Group[] }[] = [];
      for (const side of [-1,1]) {
        const arm = new THREE.Group(); arm.position.set(side*0.53,1.32,0); scene.add(arm);
        ellipsoid(0,-0.24,0,0.135,0.28,0.14,shirt,arm);
        const elbow=new THREE.Group();elbow.position.y=-0.48;arm.add(elbow);
        ellipsoid(0,0,0,0.105,0.105,0.105,skin,elbow);
        ellipsoid(0,-0.21,0,0.095,0.23,0.095,skin,elbow);
        const wrist=new THREE.Group();wrist.position.y=-0.43;elbow.add(wrist);
        ellipsoid(0,-0.09,0,0.12,0.15,0.06,skin,wrist);
        const fingers:THREE.Group[]=[];
        for(let i=0;i<4;i++){
          const finger=new THREE.Group();finger.position.set((i-1.5)*0.052,-0.19,0);wrist.add(finger);
          ellipsoid(0,-0.075,0,0.022,0.085,0.025,skin,finger);fingers.push(finger);
        }
        ellipsoid(side*0.12,-0.1,0.015,0.04,0.085,0.035,skin,wrist);
        arm.rotation.z = side*0.12; arms.push({shoulder:arm,elbow,wrist,fingers});
      }
      const base = new THREE.Mesh(new THREE.CylinderGeometry(0.95,1,0.07,64), new THREE.MeshStandardMaterial({color:'#c7d8cc'}));
      base.position.y=-0.13; scene.add(base);
      const fit = () => {
        if (stopped) return;
        const {width,height}=target.getBoundingClientRect();
        if (width < 1 || height < 1) return;
        renderer!.setSize(width,height, false);
        camera.aspect=width/height;
        // Keep both hands in frame even on a narrow phone screen.
        camera.position.z = Math.max(6.5, 5.8 / camera.aspect);
        camera.updateProjectionMatrix();
        renderer!.render(scene,camera);
      };
      redraw = fit;
      fit();
      resize = new ResizeObserver(fit); resize.observe(target);
      document.addEventListener('visibilitychange', visibilityChanged);
      const started = performance.now();
      const reducedMotion = matchMedia('(prefers-reduced-motion: reduce)').matches;
      const animate = () => {
        if (stopped) return;
        const t=(performance.now()-started)/1000;
        const elapsed=(performance.now()-gesture.current.started)/1000;
        const active=elapsed<5 && gesture.current.name !== '';
        const idle=reducedMotion ? 0 : Math.sin(t*1.7)*0.035;
        // Smoothly enter/leave the demonstration and return every joint to neutral.
        const blend=active?Math.min(1,elapsed/0.4,(5-elapsed)/0.5):0;
        const wave=reducedMotion?0:Math.sin(elapsed*7);
        arms.forEach(({shoulder,elbow,wrist,fingers},index)=>{
          const side=index===0?-1:1;
          let lift=0,bend=0,forward=0,twist=0,curl=0;
          switch(gesture.current.name){
            case 'Hello': lift=side*(1.6+wave*0.12);bend=side*(0.85+wave*0.18);twist=wave*0.55;break;
            case 'Welcome': lift=side*(0.85+Math.sin(elapsed*2)*0.22);bend=side*0.65;forward=-0.65;twist=side*0.35;break;
            case 'Yes': lift=side*0.5;bend=side*1.5;forward=-0.65;curl=1.6;twist=wave*0.65;break;
            case 'No': lift=side*0.7;bend=side*1.3;forward=-0.55;twist=wave*0.8;break;
            case 'Thank you': lift=side*0.65;bend=side*1.6;forward=-0.8+Math.sin(elapsed*2)*0.5;twist=side*0.25;break;
            case 'Help': lift=side*(0.8+Math.sin(elapsed*3)*0.16);bend=side*1.5;forward=-0.6;curl=index===1?1.5:0;break;
          }
          shoulder.rotation.set(forward*blend,0,side*(0.12+idle)*(1-blend)+lift*blend);
          elbow.rotation.set(0,0,bend*blend);
          wrist.rotation.set((gesture.current.name==='Yes'?twist:0)*blend,0,(gesture.current.name==='Yes'?0:twist)*blend);
          fingers.forEach(f=>{f.rotation.x=curl*blend;});
        });
        try { renderer!.render(scene,camera); frame=requestAnimationFrame(animate); }
        catch { stopped=true;setState('3D rendering stopped. Retry the preview; messages still work.'); }
      }; setState(''); animate();
    } catch { setState('3D preview unavailable on this device. Messages still work.'); }
    return () => { stopped=true;cancelAnimationFrame(frame); resize?.disconnect(); document.removeEventListener('visibilitychange',visibilityChanged); renderer?.domElement.removeEventListener('webglcontextlost',contextLost); scene.traverse(o=>{ if(o instanceof THREE.Mesh){o.geometry.dispose(); const mats=Array.isArray(o.material)?o.material:[o.material]; mats.forEach(m=>m.dispose());} }); renderer?.dispose(); renderer?.domElement.remove(); };
  }, [attempt]);
  // React owns the overlay; Three.js exclusively owns children of the empty mount.
  return <div className="avatar-view" role="img" aria-label="Stylized 3D human avatar. Demonstration gestures only, not sign-language translation."><div className="avatar-canvas" ref={host}/>{state && <div className="avatar-fallback" role="status"><p>{state}</p>{!state.startsWith('Loading')&&<button className="secondary" onClick={()=>setAttempt(a=>a+1)}>Retry 3D preview</button>}</div>}</div>;
}
