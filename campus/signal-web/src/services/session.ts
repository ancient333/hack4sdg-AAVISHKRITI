import { useEffect, useRef, useState } from 'react';
export interface Message { id: string; text: string; sender: string; time: number; delivered?:boolean; clientId?:string; own?:boolean; delivery?:'local'|'pending'|'server'|'delivered'|'unconfirmed' }
export function useSession() {
  const socket=useRef<WebSocket|null>(null);
  const [status,setStatus]=useState('Not connected');
  const [session,setSession]=useState('');
  const [pending,setPending]=useState(false);
  const [messages,setMessages]=useState<Message[]>([]);
  const [error,setError]=useState('');
  const [role,setRole]=useState('');
  const [sending,setSending]=useState(false);
  const waiting=useRef(new Map<string,{resolve:(ok:boolean)=>void;timer:ReturnType<typeof setTimeout>}>());
  const connectionTimer=useRef<ReturnType<typeof setTimeout>|undefined>(undefined);
  function finish(id:string,ok:boolean){const item=waiting.current.get(id);if(!item)return;clearTimeout(item.timer);waiting.current.delete(id);setMessages(prev=>prev.map(m=>m.clientId===id?{...m,delivery:ok?'delivered':'unconfirmed'}:m));item.resolve(ok);setSending(waiting.current.size>0);}
  function cancelSends(){for(const id of waiting.current.keys())finish(id,false);}
  useEffect(()=>()=>{clearTimeout(connectionTimer.current);const ws=socket.current;socket.current=null;ws?.close();for(const item of waiting.current.values()){clearTimeout(item.timer);item.resolve(false);}waiting.current.clear();},[]);
  function connect(id?:string) {
    if(id!==undefined&&!/^[a-f0-9]{12}$/i.test(id)){setError('Enter the 12-character session ID displayed on the receiving device.');return;}
    const previous=socket.current;socket.current=null;previous?.close();clearTimeout(connectionTimer.current);cancelSends();
    setError(''); setStatus('Connecting…'); setPending(false); setSession(''); setRole(id?'guest':'host');
    let ws:WebSocket;
    try{ws=new WebSocket(`${location.protocol==='https:'?'wss':'ws'}://${location.host}/ws`);}catch{setStatus('Disconnected');setError('Connection could not start. Check the app address.');return;}
    socket.current=ws;
    connectionTimer.current=setTimeout(()=>{if(socket.current!==ws)return;setError('The server did not respond. Check your network and try again.');ws.close();},10000);
    ws.onopen=()=>{if(socket.current===ws)ws.send(JSON.stringify(id?{type:'join',session:id.toLowerCase()}:{type:'create'}));};
    ws.onmessage=e=>{ if(socket.current!==ws)return; let data;try{data=JSON.parse(e.data);}catch{setError('The server returned an unreadable response.');return;}
      if(['session','waiting','paired','error'].includes(data.type))clearTimeout(connectionTimer.current);
      if(data.type==='session'){setSession(data.session);setStatus('Waiting for partner');}
      if(data.type==='pending'){setPending(true);}
      if(data.type==='waiting')setStatus('Waiting for approval');
      if(data.type==='paired'){setStatus('Connected');setPending(false);setSession(data.session);setError('');}
      if(data.type==='message'){
        const own=data.message.sender===(id?'guest':'host');
        setMessages(prev=>{
          const existing=own&&data.message.clientId?prev.find(m=>m.clientId===data.message.clientId):undefined;
          if(existing)return prev.map(m=>m===existing?{...m,...data.message,id:existing.id,own:true,delivery:'server'}:m);
          return [...prev.slice(-99),{...data.message,own}];
        });
        if(data.message.sender!==(id?'guest':'host')&&ws.readyState===WebSocket.OPEN)ws.send(JSON.stringify({type:'received',id:data.message.id}));
      }
      if(data.type==='delivered'){setMessages(prev=>prev.map(m=>m.clientId===data.clientId?{...m,delivered:true,delivery:'delivered'}:m));finish(data.clientId,true);}
      if(data.type==='left'){setStatus('Partner disconnected');setPending(false);if(waiting.current.size)setError('Partner disconnected before confirming remote delivery. Your messages remain available locally.');cancelSends();}
      if(data.type==='error'){setError(data.message);if(data.clientId)finish(data.clientId,false);if(['join','create'].includes(data.action))ws.close();}
    };
    ws.onerror=()=>{if(socket.current===ws)setError('Unable to reach the session server. Check your connection and try again.');};
    ws.onclose=()=>{if(socket.current===ws){clearTimeout(connectionTimer.current);setStatus('Disconnected');setPending(false);setSession('');cancelSends();}};
  }
  function send(text:string):Promise<boolean>{
    const ws=socket.current;
    if(!text.trim()||text.length>1000){setError('Enter a message between 1 and 1000 characters.');return Promise.resolve(false);}
    const clientId=`${Date.now()}-${Math.random().toString(36).slice(2)}`;
    const remote=status==='Connected'&&ws?.readyState===WebSocket.OPEN;
    setError('');
    setMessages(prev=>[...prev.slice(-99),{id:clientId,clientId,text:text.trim(),sender:role||'local',own:true,time:Date.now(),delivery:remote?'pending':'local'}]);
    if(!remote)return Promise.resolve(true);
    setSending(true);
    return new Promise(resolve=>{
      const timer=setTimeout(()=>{setError('Remote delivery was not confirmed. The message remains in your local conversation.');finish(clientId,false);},10000);
      waiting.current.set(clientId,{resolve,timer});
      try{ws!.send(JSON.stringify({type:'message',text:text.trim(),clientId}));}catch{setError('Remote delivery failed. The message remains available locally.');finish(clientId,false);}
    });
  }
  function approve(allow:boolean){if(socket.current?.readyState!==WebSocket.OPEN){setError('Connection lost. Create a new session to pair.');return;}socket.current.send(JSON.stringify({type:'approve',allow}));setPending(false);}
  function disconnect(){const ws=socket.current;socket.current=null;clearTimeout(connectionTimer.current);cancelSends();ws?.close();setStatus('Disconnected');setSession('');setPending(false);setError('');}
  return {status,session,pending,messages,error,role,sending,connect,send,approve,disconnect};
}
