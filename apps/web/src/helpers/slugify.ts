  const fromTR: Record<string, string> = {
  'ç': 'c',
  'ğ': 'g',
  'ı': 'i',
  'ö': 'o',
  'ş': 's',
  'ü': 'u',
  'Ç': 'c',
  'Ğ': 'g',
  'İ': 'i',
  'Ö': 'o',
  'Ş': 's',
  'Ü': 'u',
}

export function slugify(input: string): string {
  if (!input) return ''

  let s = input
    .split('')
    .map((ch) => fromTR[ch] ?? ch) // сначала турецкий маппинг
    .join('')

  return s
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .toLowerCase()
    .replace(/[^a-z0-9\s-]/g, ' ')
    .trim()
    .replace(/[\s-]+/g, '-')
}
