import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Appointment, BookingRequest, TimeSlot } from '../models/appointment.model';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class AppointmentService {
  private apiUrl = `${environment.apiUrl}/api/appointments`;

  constructor(private http: HttpClient) {}

  getAvailableSlots(branchId: number, date: string): Observable<TimeSlot[]> {
    const params = new HttpParams()
      .set('branchId', branchId.toString())
      .set('date', date);

    return this.http.get<TimeSlot[]>(`${this.apiUrl}/slots`, { params });
  }

  bookAppointment(booking: BookingRequest): Observable<Appointment> {
    return this.http.post<Appointment>(`${this.apiUrl}/book`, booking);
  }

  lookupAppointment(email: string, bookingReference: string): Observable<Appointment> {
    const params = new HttpParams()
      .set('email', email)
      .set('bookingReference', bookingReference);

    return this.http.get<Appointment>(`${this.apiUrl}/lookup`, { params });
  }

  cancelAppointment(email: string, bookingReference: string, reason?: string): Observable<Appointment> {
    let params = new HttpParams()
      .set('email', email)
      .set('bookingReference', bookingReference);

    if (reason) {
      params = params.set('reason', reason);
    }

    return this.http.put<Appointment>(`${this.apiUrl}/cancel`, null, { params });
  }
}
