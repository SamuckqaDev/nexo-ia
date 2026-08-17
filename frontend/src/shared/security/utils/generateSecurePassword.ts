const LOWERCASE = "abcdefghijkmnopqrstuvwxyz";
const UPPERCASE = "ABCDEFGHJKLMNPQRSTUVWXYZ";
const NUMBERS = "23456789";
const SYMBOLS = "!@#$%&*+-_?";
const ALL_CHARACTERS = LOWERCASE + UPPERCASE + NUMBERS + SYMBOLS;

function secureIndex(maximum: number): number {
  const values = new Uint32Array(1);
  const limit = Math.floor(0x1_0000_0000 / maximum) * maximum;
  do crypto.getRandomValues(values); while (values[0] >= limit);
  return values[0] % maximum;
}

function pick(source: string): string {
  return source[secureIndex(source.length)];
}

export function generateSecurePassword(length = 16): string {
  const characters: string[] = [
    pick(LOWERCASE), pick(UPPERCASE), pick(NUMBERS), pick(SYMBOLS)
  ];
  while (characters.length < length) characters.push(pick(ALL_CHARACTERS));
  for (let index = characters.length - 1; index > 0; index--) {
    const target = secureIndex(index + 1);
    [characters[index], characters[target]] = [characters[target], characters[index]];
  }
  return characters.join("");
}
