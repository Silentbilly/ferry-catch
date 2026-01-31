export type RouteDto = {
  id: string
  from: string
  to: string
  operator: string
}

export type RouteWithNextDto = RouteDto & {
  nextMinutesUntil?: number | null
  nextDepartureTime?: string | null // ISO-8601 date-time string
}

export type StopDto = {
  stopName: string
  sequence: number
  time: string // ISO-8601 date-time string
}

export type TripDto = {
  tripId: string
  operator: string
  from: string
  to: string
  departureTime: string // ISO-8601 date-time string
  arrivalTime: string // ISO-8601 date-time string
  stops: StopDto[]
}

export type NextResponse = {
  trip: TripDto
  minutesUntil: number
}

export type TimetableResponse = {
  route: RouteDto
  date: string // yyyy-MM-dd
  trips: TripDto[]
}
