import { createServer } from 'node:http';
import { readFile } from 'node:fs/promises';
import { resolve, extname, sep } from 'node:path';
import { networkInterfaces } from 'node:os';
import { attachSessions } from './session.mjs';
import { networkAddresses } from './network.mjs';
const production=process.argv.includes('--production');
const port=Number(process.env.PORT||3001);const root=resolve('dist');
const mime={'.html':'text/html; charset=utf-8','.js':'text/javascript','.css':'text/css','.svg':'image/svg+xml','.png':'image/png','.ico':'image/x-icon','.wasm':'application/wasm','.task':'application/octet-stream'};
const server=createServer(async(req,res)=>{
 if(req.url?.split('?')[0]==='/api/network'){
  res.writeHead(200,{'Content-Type':'application/json','Cache-Control':'no-store'});
  res.end(JSON.stringify({addresses:networkAddresses(production?port:5173)}));return;
 }
 if(!production){res.writeHead(200,{'Content-Type':'application/json'});res.end(JSON.stringify({status:'ok'}));return;}
 try{const pathname=decodeURIComponent(new URL(req.url,'http://localhost').pathname);let file=resolve(root,'.'+pathname);if(file!==root&&!file.startsWith(root+sep)){res.writeHead(403);res.end();return;}if(!extname(file))file=resolve(root,'index.html');const body=await readFile(file);res.writeHead(200,{'Content-Type':mime[extname(file)]||'application/octet-stream','X-Content-Type-Options':'nosniff','Referrer-Policy':'no-referrer'});res.end(body);}catch{res.writeHead(404);res.end('Not found');}
});
attachSessions(server);
server.listen(port,'0.0.0.0',()=>{console.log(`Signal ${production?'app':'session server'}: http://localhost:${port}`);for(const addresses of Object.values(networkInterfaces()))for(const a of addresses||[])if(a.family==='IPv4'&&!a.internal)console.log(`LAN app address: http://${a.address}:${production?port:5173}`);});
