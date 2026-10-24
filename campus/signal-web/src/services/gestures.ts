export const DEMO_GESTURES = ['Hello', 'Welcome', 'Yes', 'No', 'Thank you', 'Help'] as const;
export function matchGesture(text: string): string {
  const normalized = text.toLowerCase().trim().replace(/[.!?,]+$/g, '').replace(/\s+/g, ' ');
  return DEMO_GESTURES.find(name => name.toLowerCase() === normalized) || '';
}

export interface AnimationPlan {
  text: string;
  queue: string[];
  unsupported: string[];
}
// Lexical matching in source order, not linguistic translation. Phrases win
// over individual words; no unknown word is substituted with another gesture.
export function planAnimations(text: string): AnimationPlan {
  const tokens = text.match(/[\p{L}\p{N}]+(?:['’][\p{L}]+)*/gu) || [];
  const queue: string[] = [], unsupported: string[] = [];
  const vocabulary = [...DEMO_GESTURES].sort((a,b) => b.split(' ').length - a.split(' ').length);
  for (let index = 0; index < tokens.length;) {
    const match = vocabulary.find(phrase => phrase.toLowerCase() === tokens.slice(index, index + phrase.split(' ').length).join(' ').toLowerCase());
    if (match) { queue.push(match); index += match.split(' ').length; }
    else { unsupported.push(tokens[index]); index++; }
  }
  if (!tokens.length && text.trim()) unsupported.push(text.trim());
  return { text, queue, unsupported };
}
