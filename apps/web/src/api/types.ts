export type RouteDto = {
  id: string
  from: string
  to: string
  operator: string
}

export type StopDto = {
  stopName: string
  sequence: number
  time: string
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

export type TimetableResponse = {
  route: RouteDto
  date: string
  trips: TripDto[]
}
