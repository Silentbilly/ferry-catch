import type { Lang } from '../i18n'
import { getLocale, messages } from '../i18n'

export type DateInput = string | number | Date | null | undefined

export function parseDate(input: DateInput): Date | null {
  if (input == null || input === '') return null

  const d = input instanceof Date ? input : new Date(input)
  return Number.isNaN(d.getTime()) ? null : d
}

export function formatHHmm(
  input: DateInput,
  mode: 'local' | 'utc' = 'local',
  locales?: Intl.LocalesArgument,
): string {
  const d = parseDate(input)
  if (!d) return ''

  const options: Intl.DateTimeFormatOptions = {
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
    ...(mode === 'utc' ? { timeZone: 'UTC' } : {}),
  }

  return new Intl.DateTimeFormat(locales, options).format(d)
}

export function formatHHmmByLang(
  input: DateInput,
  lang: Lang,
  mode: 'local' | 'utc' = 'local',
): string {
  return formatHHmm(input, mode, getLocale(lang))
}

export function formatYYYYMMDD(
  input: DateInput,
  mode: 'local' | 'utc' = 'local',
): string {
  const d = parseDate(input)
  if (!d) return ''

  const year = mode === 'utc' ? d.getUTCFullYear() : d.getFullYear()
  const month = (mode === 'utc' ? d.getUTCMonth() : d.getMonth()) + 1
  const day = mode === 'utc' ? d.getUTCDate() : d.getDate()

  const mm = String(month).padStart(2, '0')
  const dd = String(day).padStart(2, '0')
  return `${year}-${mm}-${dd}`
}

export function timeUntil(isoTime: string, lang: Lang = 'en'): string {
  const depMs = new Date(isoTime).getTime()
  const nowMs = Date.now()

  const totalMinutes = Math.ceil((depMs - nowMs) / 1000 / 60)
  const dict = messages[lang]

  if (totalMinutes < 0) return dict.departed
  if (totalMinutes < 60) return `${totalMinutes} ${dict.min}`

  const h = Math.floor(totalMinutes / 60)
  const m = totalMinutes % 60
  return `${h}${dict.hour} ${m} ${dict.min}`
}