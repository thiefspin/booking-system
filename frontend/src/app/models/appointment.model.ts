export interface Appointment {
  id?: number;
  bookingReference: string;
  branchId: number;
  customerFirstName: string;
  customerLastName: string;
  customerEmail: string;
  customerPhone: string;
  appointmentDateTime: string;
  durationMinutes: number;
  purpose?: string;
  notes?: string;
  status: AppointmentStatus;
}

export enum AppointmentStatus {
  PENDING = 'PENDING',
  CONFIRMED = 'CONFIRMED',
  CANCELLED = 'CANCELLED',
  COMPLETED = 'COMPLETED',
  NO_SHOW = 'NO_SHOW'
}

export interface BookingRequest {
  branchId: number;
  firstName: string;
  lastName: string;
  email: string;
  phoneNumber: string;
  appointmentDateTime: string;
  durationMinutes: number;
  purpose?: string;
  notes?: string;
}

export interface TimeSlot {
  startTime: string;
  endTime: string;
  available: boolean;
  currentBookings: number;
  maxBookings: number;
}

export interface ApiErrorResponse {
  status: number;
  message: string;
  timestamp: string;
}
