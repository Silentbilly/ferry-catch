export type RouteDto = {
  id: string
  from: string
  to: string
  operator: string
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
  departureTime: string
  arrivalTime: string
  stops: StopDto[]
}

export type NextResponse = {
  trip: TripDto
  minutesUntil: number
}

export type SearchResponse = {
  trip: TripDto
  minutesUntil: number
}

export type TimetableResponse = {
  route: RouteDto | { id: string | null; from: string; to: string; operator: string }
  date: string
  trips: TripDto[]
}
