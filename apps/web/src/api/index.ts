// src/api/index.ts
import { api } from './client'
import type {
  NextResponse,
  RouteDto,
  TimetableResponse,
  SearchResponse,
} from './types'

export const listRoutes = () =>
  api.get<RouteDto[]>('/routes')

// NEW
export const listStops = (q?: { operator?: string }) =>
  api.get<string[]>('/stops', q)

// NEW (UI primary)
export const searchNext = (q: { from: string; to: string; operator?: string }) =>
  api.get<SearchResponse>('/search', q)

// optional: keep old endpoints for debug
export const getNextDeparture = (q: { from: string; to: string; operator?: string }) =>
  api.get<NextResponse>('/next', q)

export const getTimetable = (q: { from: string; to: string; operator?: string; date?: string }) =>
  api.get<TimetableResponse>('/timetable', q)

export const getUpcomingTimetable = (q: { from: string; to: string; operator?: string; limit: number }) =>
  api.get<TimetableResponse>('/timetable/upcoming', q)
