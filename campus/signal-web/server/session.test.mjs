import test from 'node:test';
import assert from 'node:assert/strict';
import { createServer } from 'node:http';
import { once } from 'node:events';
import { WebSocket } from 'ws';
import { attachSessions } from './session.mjs';
import { matchGesture, planAnimations } from '../src/services/gestures.ts';
import { networkAddresses } from './network.mjs';

test('sentences retain order, phrase boundaries, repetitions and unsupported vocabulary',()=>{
 assert.deepEqual(planAnimations('Hello! Thank you, please HELP.'),{text:'Hello! Thank you, please HELP.',queue:['Hello','Thank you','Help'],unsupported:['please']});
 assert.deepEqual(planAnimations('How are you?').unsupported,['How','are','you']);
 assert.deepEqual(planAnimations('How are you?').queue,[]);
 assert.deepEqual(planAnimations('yes yes, yesterday').queue,['Yes','Yes']);
 assert.deepEqual(planAnimations('你好!').unsupported,['你好']);
});

test('network discovery excludes loopback and link-local interfaces',()=>{
 assert.deepEqual(networkAddresses(5173,{loopback:[{family:'IPv4',internal:true,address:'127.0.0.1'}],wifi:[{family:'IPv4',internal:false,address:'192.168.1.8'},{family:'IPv4',internal:false,address:'169.254.1.2'}]}),['http://192.168.1.8:5173']);
});

test('demo matching accepts supported phrases but does not translate arbitrary sentences',()=>{
 assert.equal(matchGesture('  HELLO!  '),'Hello');
 assert.equal(matchGesture('Thank   you.'),'Thank you');
 assert.equal(matchGesture('Please help me with this sentence'),'');
 assert.equal(matchGesture('yesterday'),'');
 assert.equal(matchGesture(''),'');
});

test('two clients pair by approval, exchange text both ways, reject invalid messages, and disconnect',async()=>{
 const server=createServer();const wss=attachSessions(server);server.listen(0,'127.0.0.1');await once(server,'listening');const url=`ws://127.0.0.1:${server.address().port}/ws`;
 const clients=[];
 async function client(){const ws=new WebSocket(url);clients.push(ws);await once(ws,'open');return ws;}
 function receive(ws){return new Promise((resolve,reject)=>{const timer=setTimeout(()=>{ws.off('message',handler);reject(new Error('Timed out waiting for event'));},2500);const handler=raw=>{clearTimeout(timer);resolve(JSON.parse(raw));};ws.once('message',handler);});}
 function emit(ws,data){ws.send(JSON.stringify(data));}
 try{
 const host=await client(),guest=await client(),outsider=await client();
 let received=receive(host);emit(host,{type:'create'});const {session}=await received;assert.match(session,/^[a-f0-9]{12}$/);
 let pending=receive(host);received=receive(guest);emit(guest,{type:'join',session});assert.equal((await received).type,'waiting');assert.equal((await pending).type,'pending');
 received=receive(guest);emit(guest,{type:'message',text:'Before approval'});assert.equal((await received).type,'error');
 received=receive(outsider);emit(outsider,{type:'join',session});assert.equal((await received).type,'error');
 let hostEvent=receive(host),guestEvent=receive(guest);emit(host,{type:'approve',allow:true});assert.equal((await hostEvent).type,'paired');assert.equal((await guestEvent).type,'paired');
 for(const [sender,role] of [[guest,'guest'],[host,'host']]){
  hostEvent=receive(host);guestEvent=receive(guest);
  const clientId=`test-${role}`;
  emit(sender,{type:'message',text:'Hello <script> & नमस्ते',clientId});
  const a=await hostEvent,b=await guestEvent;assert.deepEqual(a,b);assert.equal(a.message.sender,role);assert.equal(a.message.text,'Hello <script> & नमस्ते');
  // The sender cannot forge a receipt. Only the opposite endpoint can confirm delivery.
  received=receive(sender);emit(sender,{type:'received',id:a.message.id});assert.equal((await received).type,'error');
  received=receive(sender);emit(sender===host?guest:host,{type:'received',id:a.message.id});
  assert.deepEqual(await received,{type:'delivered',id:a.message.id,clientId});
 }
 for(const text of ['', 'x'.repeat(1001)]){received=receive(guest);emit(guest,{type:'message',text});assert.equal((await received).type,'error');}
 received=receive(host);guest.close();assert.equal((await received).type,'left');
 received=receive(outsider);emit(outsider,{type:'join',session:'missing'});assert.equal((await received).type,'error');
 pending=receive(host);received=receive(outsider);emit(outsider,{type:'join',session});assert.equal((await received).type,'waiting');assert.equal((await pending).type,'pending');
 received=receive(outsider);emit(host,{type:'approve',allow:false});assert.match((await received).message,/declined/);
 }finally{clients.forEach(ws=>ws.terminate());await new Promise(r=>wss.close(r));await new Promise(r=>server.close(r));}
});
