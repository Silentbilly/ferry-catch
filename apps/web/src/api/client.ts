export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'

export type QueryValue = string | number | boolean | null | undefined
export type Query = Record<string, QueryValue>

type JsonPrimitive = string | number | boolean | null
export type JsonValue = JsonPrimitive | JsonValue[] | { [k: string]: JsonValue }

export class ApiError extends Error {
  readonly status: number
  readonly url: string
  readonly body: unknown

  constructor(args: { status: number; url: string; message: string; body?: unknown }) {
    super(args.message)
    this.name = 'ApiError'
    this.status = args.status
    this.url = args.url
    this.body = args.body
  }
}

export type ApiClientConfig = {
  baseUrl?: string // dev: '/api/v1' (идёт в Vite proxy)
  timeoutMs?: number
  headers?: Record<string, string>
}

const defaultConfig: Required<Pick<ApiClientConfig, 'baseUrl' | 'timeoutMs'>> = {
  baseUrl: '/api/v1',
  timeoutMs: 15_000,
}

function joinUrl(baseUrl: string, path: string): string {
  if (!baseUrl) return path
  if (baseUrl.endsWith('/') && path.startsWith('/')) return baseUrl + path.slice(1)
  if (!baseUrl.endsWith('/') && !path.startsWith('/')) return `${baseUrl}/${path}`
  return baseUrl + path
}

function buildUrl(baseUrl: string, path: string, query?: Query): string {
  const fullPath = joinUrl(baseUrl, path)
  if (!query) return fullPath

  const sp = new URLSearchParams()
  for (const [k, v] of Object.entries(query)) {
    if (v === undefined || v === null) continue
    sp.set(k, String(v))
  }

  const qs = sp.toString()
  return qs ? `${fullPath}?${qs}` : fullPath
}

async function readBody(res: Response): Promise<{ text: string; json: unknown | null }> {
  const text = await res.text()
  if (!text) return { text: '', json: null }
  try {
    return { text, json: JSON.parse(text) }
  } catch {
    return { text, json: null }
  }
}

export function createApiClient(cfg: ApiClientConfig = {}) {
  const baseUrl = cfg.baseUrl ?? defaultConfig.baseUrl
  const timeoutMs = cfg.timeoutMs ?? defaultConfig.timeoutMs
  const defaultHeaders = cfg.headers ?? {}

  async function request<TResponse>(args: {
    method: HttpMethod
    path: string
    query?: Query
    json?: JsonValue
    headers?: Record<string, string>
    signal?: AbortSignal
  }): Promise<TResponse> {
    const url = buildUrl(baseUrl, args.path, args.query)

    const controller = new AbortController()
    const timer = window.setTimeout(() => controller.abort(), timeoutMs)

    const headers = new Headers({ ...defaultHeaders, ...(args.headers ?? {}) })

    let body: BodyInit | undefined
    if (args.json !== undefined) {
      headers.set('Content-Type', 'application/json')
      body = JSON.stringify(args.json)
    }

    try {
      const res = await fetch(url, {
        method: args.method,
        headers,
        body,
        signal: args.signal ?? controller.signal,
      })

      const { text, json } = await readBody(res)

      if (!res.ok) {
        const message =
          (json && typeof json === 'object' && json !== null && ('message' in json || 'error' in json)
            ? String((json as any).message ?? (json as any).error)
            : '') || `HTTP ${res.status} ${res.statusText}`

        throw new ApiError({ status: res.status, url, message, body: json ?? text })
      }

      if (res.status === 204) return undefined as TResponse
      if (json === null) return undefined as TResponse
      return json as TResponse
    } catch (e: any) {
      if (e?.name === 'AbortError') {
        throw new ApiError({ status: 0, url, message: `Request timed out after ${timeoutMs}ms` })
      }
      throw e
    } finally {
      window.clearTimeout(timer)
    }
  }

  return {
    request,
    get: <T>(path: string, query?: Query) => request<T>({ method: 'GET', path, query }),
    post: <T>(path: string, json?: JsonValue, query?: Query) => request<T>({ method: 'POST', path, json, query }),
  }
}

export const api = createApiClient()
