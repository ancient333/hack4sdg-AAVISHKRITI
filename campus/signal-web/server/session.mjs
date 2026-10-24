import { randomBytes, randomUUID } from 'node:crypto';
import { WebSocketServer, WebSocket } from 'ws';

export function attachSessions(server) {
 const rooms=new Map();
 const wss=new WebSocketServer({server,path:'/ws',maxPayload:8192});
 const send=(ws,data)=>{if(ws?.readyState===WebSocket.OPEN)ws.send(JSON.stringify(data));};
 const fail=(ws,message)=>send(ws,{type:'error',message,action:ws.action,clientId:ws.clientId});
 wss.on('connection',(ws,request)=>{
  // Browser origins must match the serving host. Non-browser test clients can omit Origin.
  if(request.headers.origin){try{if(new URL(request.headers.origin).host!==request.headers.host){ws.close(1008,'Origin not allowed');return;}}catch{ws.close(1008);return;}}
  ws.alive=true;ws.on('pong',()=>{ws.alive=true;});
  ws.on('message',raw=>{
   try {
    const data=JSON.parse(raw.toString());
    if(!data||typeof data!=='object')return fail(ws,'Invalid request.');
    ws.action=data.type;
    ws.clientId=typeof data.clientId==='string'&&data.clientId.length<=80?data.clientId:undefined;
    if(data.type==='create'){
     if(ws.room)return fail(ws,'Leave the current session first.');
     if(rooms.size>=500)return fail(ws,'Server is busy. Please try again later.');
     const id=randomBytes(6).toString('hex');const room={id,host:ws,guest:null,pending:null,deliveries:new Map()};rooms.set(id,room);ws.room=room;ws.role='host';send(ws,{type:'session',session:id});
    }else if(data.type==='join'){
     if(ws.room)return fail(ws,'Leave the current session first.');
     const room=typeof data.session==='string'?rooms.get(data.session):null;
     if(!room)return fail(ws,'Session not found. Check the ID or ask your partner to create a new session.');
     if(room.guest||room.pending)return fail(ws,'This session already has a partner or a pending request.');
     room.pending=ws;ws.room=room;ws.role='pending';send(ws,{type:'waiting'});send(room.host,{type:'pending'});
    }else if(data.type==='approve'){
     const room=ws.room;if(!room||ws!==room.host||!room.pending)return fail(ws,'No request to approve.');
     const guest=room.pending;room.pending=null;
     if(data.allow===true){room.guest=guest;guest.role='guest';[room.host,guest].forEach(client=>send(client,{type:'paired',session:room.id}));}
     else{guest.room=null;fail(guest,'The receiver declined the connection.');guest.close(1000,'Declined');}
    }else if(data.type==='message'){
     const room=ws.room;if(!room||!room.guest||!['host','guest'].includes(ws.role))return fail(ws,'Pair with a partner before sending.');
     const receiver=ws===room.host?room.guest:room.host;
     if(receiver.readyState!==WebSocket.OPEN)return fail(ws,'Your partner disconnected. Reconnect before sending.');
     if(typeof data.text!=='string'||!data.text.trim()||data.text.length>1000)return fail(ws,'Messages must contain 1–1000 characters.');
     const now=Date.now();ws.times=(ws.times||[]).filter(t=>now-t<1000);if(ws.times.length>=10)return fail(ws,'Too many messages. Please wait a moment.');ws.times.push(now);
     const message={id:randomUUID(),text:data.text.trim(),sender:ws.role,time:now,clientId:ws.clientId};
     if(room.deliveries.size>=100)room.deliveries.delete(room.deliveries.keys().next().value);
     room.deliveries.set(message.id,{sender:ws,receiver,clientId:ws.clientId});
     [room.host,room.guest].forEach(client=>send(client,{type:'message',message}));
    }else if(data.type==='received'){
     const room=ws.room;const delivery=room?.deliveries.get(data.id);
     if(!delivery||delivery.receiver!==ws)return fail(ws,'Invalid delivery receipt.');
     room.deliveries.delete(data.id);
     send(delivery.sender,{type:'delivered',id:data.id,clientId:delivery.clientId});
    }else fail(ws,'Unknown request.');
   }catch{fail(ws,'Invalid request.');}
  });
  ws.on('error',()=>{});
  ws.on('close',()=>{const room=ws.room;if(!room)return;if(ws===room.host){rooms.delete(room.id);for(const client of [room.guest,room.pending]){if(client){client.room=null;send(client,{type:'left'});client.close(1000,'Session ended');}}}else{if(room.guest===ws)room.guest=null;if(room.pending===ws)room.pending=null;send(room.host,{type:'left'});}});
 });
 const heartbeat=setInterval(()=>{for(const ws of wss.clients){if(!ws.alive){ws.terminate();continue;}ws.alive=false;ws.ping();}},30000);heartbeat.unref();wss.on('close',()=>clearInterval(heartbeat));
 return wss;
}
