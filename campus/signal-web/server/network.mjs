import { networkInterfaces } from 'node:os';

export function networkAddresses(port, interfaces = networkInterfaces()) {
  return [...new Set(Object.entries(interfaces)
    .sort(([a], [b]) => Number(/virtual|vpn|docker|vEthernet/i.test(a)) - Number(/virtual|vpn|docker|vEthernet/i.test(b)))
    .flatMap(([, entries]) => (entries || []).filter(a => a.family === 'IPv4' && !a.internal && !a.address.startsWith('169.254.')).map(a => `http://${a.address}:${port}`)))];
}
