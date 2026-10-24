import { useCallback, useEffect, useRef, useState } from 'react';
import { planAnimations } from './gestures';
import type { Message } from './session';

export function useAnimationQueue(messages: Message[], enabled: boolean) {
  const seen = useRef(new Set<string>());
  const [queue, setQueue] = useState<{ name: string; key: number }[]>([]);
  const sequence = useRef(0);
  useEffect(() => {
    const additions: {name:string;key:number}[] = [];
    for (const message of messages) {
      if (seen.current.has(message.id)) continue;
      seen.current.add(message.id);
      const plan = planAnimations(message.text);
      for (const name of plan.queue) additions.push({ name, key: ++sequence.current });
      // Never substitute one generic gesture for an unsupported sentence.
      // Only play named demo gestures that exist in the explicit vocabulary.
    }
    // History is bounded; retain only identities that can still be echoed.
    seen.current = new Set(messages.map(message => message.id));
    if (additions.length) setQueue(current => [...current, ...additions]);
  }, [messages]);
  const current = enabled ? queue[0] : undefined;
  useEffect(() => {
    if (!current) return;
    const timer = setTimeout(() => setQueue(items => items[0]?.key === current.key ? items.slice(1) : items), 5200);
    return () => clearTimeout(timer);
  }, [current?.key, enabled]);
  const play = useCallback((name: string) => setQueue([{ name, key: ++sequence.current }]), []);
  const reset = useCallback(() => setQueue([]), []);
  return { demo: current?.name || '', replay: current?.key || 0, pending: queue.length, play, reset };
}
