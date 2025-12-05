import { TestBed } from "@angular/core/testing";
import {
  HttpClientTestingModule,
  HttpTestingController,
} from "@angular/common/http/testing";
import { AppointmentService } from "./appointment.service";
import {
  Appointment,
  BookingRequest,
  TimeSlot,
  AppointmentStatus,
} from "../models/appointment.model";
import { environment } from "../../environments/environment";

describe("AppointmentService", () => {
  let service: AppointmentService;
  let httpMock: HttpTestingController;
  const apiUrl = `${environment.apiUrl}/api/appointments`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [AppointmentService],
    });
    service = TestBed.inject(AppointmentService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe("getAvailableSlots", () => {
    it("should retrieve available time slots", () => {
      // given
      const branchId = 1;
      const date = "2024-01-15";
      const mockSlots: TimeSlot[] = [
        {
          startTime: "2024-01-15T09:00:00",
          endTime: "2024-01-15T09:30:00",
          available: true,
          currentBookings: 1,
          maxBookings: 3,
        },
        {
          startTime: "2024-01-15T09:30:00",
          endTime: "2024-01-15T10:00:00",
          available: true,
          currentBookings: 0,
          maxBookings: 3,
        },
        {
          startTime: "2024-01-15T10:00:00",
          endTime: "2024-01-15T10:30:00",
          available: false,
          currentBookings: 3,
          maxBookings: 3,
        },
      ];

      // when
      service.getAvailableSlots(branchId, date).subscribe((slots) => {
        // then
        expect(slots).toEqual(mockSlots);
        expect(slots.length).toBe(3);
        expect(slots[0].available).toBe(true);
        expect(slots[2].available).toBe(false);
      });

      const req = httpMock.expectOne(
        `${apiUrl}/slots?branchId=${branchId}&date=${date}`,
      );
      expect(req.request.method).toBe("GET");
      expect(req.request.params.get("branchId")).toBe(branchId.toString());
      expect(req.request.params.get("date")).toBe(date);
      req.flush(mockSlots);
    });

    it("should handle empty slots response", () => {
      // given
      const branchId = 2;
      const date = "2024-01-16";
      const mockSlots: TimeSlot[] = [];

      // when
      service.getAvailableSlots(branchId, date).subscribe((slots) => {
        // then
        expect(slots).toEqual([]);
        expect(slots.length).toBe(0);
      });

      const req = httpMock.expectOne(
        `${apiUrl}/slots?branchId=${branchId}&date=${date}`,
      );
      req.flush(mockSlots);
    });

    it("should handle invalid branch id", () => {
      // given
      const branchId = 999;
      const date = "2024-01-15";

      // when
      service.getAvailableSlots(branchId, date).subscribe({
        next: () => fail("should have failed"),
        error: (error) => {
          // then
          expect(error.status).toBe(404);
          expect(error.statusText).toBe("Branch not found");
        },
      });

      const req = httpMock.expectOne(
        `${apiUrl}/slots?branchId=${branchId}&date=${date}`,
      );
      req.flush(null, { status: 404, statusText: "Branch not found" });
    });

    it("should handle server error", () => {
      // given
      const branchId = 1;
      const date = "2024-01-15";

      // when
      service.getAvailableSlots(branchId, date).subscribe({
        next: () => fail("should have failed"),
        error: (error) => {
          // then
          expect(error.status).toBe(500);
        },
      });

      const req = httpMock.expectOne(
        `${apiUrl}/slots?branchId=${branchId}&date=${date}`,
      );
      req.flush(null, { status: 500, statusText: "Internal Server Error" });
    });
  });

  describe("bookAppointment", () => {
    it("should successfully book an appointment", () => {
      // given
      const bookingRequest: BookingRequest = {
        branchId: 1,
        firstName: "John",
        lastName: "Doe",
        email: "john.doe@example.com",
        phoneNumber: "555-0123",
        appointmentDateTime: "2024-01-15T09:00:00",
        durationMinutes: 30,
        purpose: "Consultation",
        notes: "First time visit",
      };

      const mockAppointment: Appointment = {
        id: 123,
        bookingReference: "BR20240115001",
        branchId: 1,
        customerFirstName: "John",
        customerLastName: "Doe",
        customerEmail: "john.doe@example.com",
        customerPhone: "555-0123",
        appointmentDateTime: "2024-01-15T09:00:00",
        durationMinutes: 30,
        purpose: "Consultation",
        notes: "First time visit",
        status: AppointmentStatus.CONFIRMED,
      };

      // when
      service.bookAppointment(bookingRequest).subscribe((appointment) => {
        // then
        expect(appointment).toEqual(mockAppointment);
        expect(appointment.bookingReference).toBe("BR20240115001");
        expect(appointment.status).toBe(AppointmentStatus.CONFIRMED);
      });

      const req = httpMock.expectOne(`${apiUrl}/book`);
      expect(req.request.method).toBe("POST");
      expect(req.request.body).toEqual(bookingRequest);
      req.flush(mockAppointment);
    });

    it("should handle booking without optional fields", () => {
      // given
      const bookingRequest: BookingRequest = {
        branchId: 2,
        firstName: "Jane",
        lastName: "Smith",
        email: "jane.smith@example.com",
        phoneNumber: "555-0456",
        appointmentDateTime: "2024-01-16T14:00:00",
        durationMinutes: 30,
      };

      const mockAppointment: Appointment = {
        id: 124,
        bookingReference: "BR20240116002",
        branchId: 2,
        customerFirstName: "Jane",
        customerLastName: "Smith",
        customerEmail: "jane.smith@example.com",
        customerPhone: "555-0456",
        appointmentDateTime: "2024-01-16T14:00:00",
        durationMinutes: 30,
        status: AppointmentStatus.CONFIRMED,
      };

      // when
      service.bookAppointment(bookingRequest).subscribe((appointment) => {
        // then
        expect(appointment.purpose).toBeUndefined();
        expect(appointment.notes).toBeUndefined();
      });

      const req = httpMock.expectOne(`${apiUrl}/book`);
      req.flush(mockAppointment);
    });

    it("should handle validation error", () => {
      // given
      const invalidRequest: BookingRequest = {
        branchId: 1,
        firstName: "",
        lastName: "",
        email: "invalid-email",
        phoneNumber: "",
        appointmentDateTime: "2024-01-15T09:00:00",
        durationMinutes: 30,
      };

      // when
      service.bookAppointment(invalidRequest).subscribe({
        next: () => fail("should have failed"),
        error: (error) => {
          // then
          expect(error.status).toBe(400);
          expect(error.error.message).toBe("Invalid booking information");
        },
      });

      const req = httpMock.expectOne(`${apiUrl}/book`);
      req.flush(
        { message: "Invalid booking information" },
        { status: 400, statusText: "Bad Request" },
      );
    });

    it("should handle slot unavailable error", () => {
      // given
      const bookingRequest: BookingRequest = {
        branchId: 1,
        firstName: "John",
        lastName: "Doe",
        email: "john.doe@example.com",
        phoneNumber: "555-0123",
        appointmentDateTime: "2024-01-15T09:00:00",
        durationMinutes: 30,
      };

      // when
      service.bookAppointment(bookingRequest).subscribe({
        next: () => fail("should have failed"),
        error: (error) => {
          // then
          expect(error.status).toBe(409);
          expect(error.error.message).toBe("Time slot is no longer available");
        },
      });

      const req = httpMock.expectOne(`${apiUrl}/book`);
      req.flush(
        { message: "Time slot is no longer available" },
        { status: 409, statusText: "Conflict" },
      );
    });
  });

  describe("lookupAppointment", () => {
    it("should retrieve appointment by email and reference", () => {
      // given
      const email = "john.doe@example.com";
      const bookingReference = "BR20240115001";
      const mockAppointment: Appointment = {
        id: 123,
        bookingReference: bookingReference,
        branchId: 1,
        customerFirstName: "John",
        customerLastName: "Doe",
        customerEmail: email,
        customerPhone: "555-0123",
        appointmentDateTime: "2024-01-15T09:00:00",
        durationMinutes: 30,
        status: AppointmentStatus.CONFIRMED,
      };

      // when
      service
        .lookupAppointment(email, bookingReference)
        .subscribe((appointment) => {
          // then
          expect(appointment).toEqual(mockAppointment);
          expect(appointment.customerEmail).toBe(email);
          expect(appointment.bookingReference).toBe(bookingReference);
        });

      const req = httpMock.expectOne(
        `${apiUrl}/lookup?email=${email}&bookingReference=${bookingReference}`,
      );
      expect(req.request.method).toBe("GET");
      expect(req.request.params.get("email")).toBe(email);
      expect(req.request.params.get("bookingReference")).toBe(bookingReference);
      req.flush(mockAppointment);
    });

    it("should handle appointment not found", () => {
      // given
      const email = "nonexistent@example.com";
      const bookingReference = "INVALID001";

      // when
      service.lookupAppointment(email, bookingReference).subscribe({
        next: () => fail("should have failed"),
        error: (error) => {
          // then
          expect(error.status).toBe(404);
          expect(error.statusText).toBe("Appointment not found");
        },
      });

      const req = httpMock.expectOne(
        `${apiUrl}/lookup?email=${email}&bookingReference=${bookingReference}`,
      );
      req.flush(null, { status: 404, statusText: "Appointment not found" });
    });

    it("should handle special characters in email", () => {
      // given
      const email = "user+test@example.com";
      const bookingReference = "BR20240115002";
      const mockAppointment: Appointment = {
        id: 124,
        bookingReference: bookingReference,
        branchId: 1,
        customerFirstName: "User",
        customerLastName: "Test",
        customerEmail: email,
        customerPhone: "555-0789",
        appointmentDateTime: "2024-01-15T10:00:00",
        durationMinutes: 30,
        status: AppointmentStatus.CONFIRMED,
      };

      // when
      service
        .lookupAppointment(email, bookingReference)
        .subscribe((appointment) => {
          // then
          expect(appointment.customerEmail).toBe(email);
        });

      const req = httpMock.expectOne((req) =>
        req.url.includes(`${apiUrl}/lookup`),
      );
      expect(req.request.params.get("email")).toBe(email);
      req.flush(mockAppointment);
    });
  });

  describe("cancelAppointment", () => {
    it("should cancel appointment with reason", () => {
      // given
      const email = "john.doe@example.com";
      const bookingReference = "BR20240115001";
      const reason = "Unable to attend";
      const mockCancelledAppointment: Appointment = {
        id: 123,
        bookingReference: bookingReference,
        branchId: 1,
        customerFirstName: "John",
        customerLastName: "Doe",
        customerEmail: email,
        customerPhone: "555-0123",
        appointmentDateTime: "2024-01-15T09:00:00",
        durationMinutes: 30,
        status: AppointmentStatus.CANCELLED,
      };

      // when
      service
        .cancelAppointment(email, bookingReference, reason)
        .subscribe((appointment) => {
          // then
          expect(appointment.status).toBe(AppointmentStatus.CANCELLED);
        });

      const req = httpMock.expectOne(
        (req) =>
          req.url === `${apiUrl}/cancel` &&
          req.params.get("email") === email &&
          req.params.get("bookingReference") === bookingReference &&
          req.params.get("reason") === reason,
      );
      expect(req.request.method).toBe("PUT");
      expect(req.request.params.get("email")).toBe(email);
      expect(req.request.params.get("bookingReference")).toBe(bookingReference);
      expect(req.request.params.get("reason")).toBe(reason);
      expect(req.request.body).toBeNull();
      req.flush(mockCancelledAppointment);
    });

    it("should cancel appointment without reason", () => {
      // given
      const email = "jane.smith@example.com";
      const bookingReference = "BR20240116002";
      const mockCancelledAppointment: Appointment = {
        id: 124,
        bookingReference: bookingReference,
        branchId: 2,
        customerFirstName: "Jane",
        customerLastName: "Smith",
        customerEmail: email,
        customerPhone: "555-0456",
        appointmentDateTime: "2024-01-16T14:00:00",
        durationMinutes: 30,
        status: AppointmentStatus.CANCELLED,
      };

      // when
      service
        .cancelAppointment(email, bookingReference)
        .subscribe((appointment) => {
          // then
          expect(appointment.status).toBe(AppointmentStatus.CANCELLED);
        });

      const req = httpMock.expectOne(
        `${apiUrl}/cancel?email=${email}&bookingReference=${bookingReference}`,
      );
      expect(req.request.method).toBe("PUT");
      expect(req.request.params.has("reason")).toBe(false);
      req.flush(mockCancelledAppointment);
    });

    it("should handle appointment not found for cancellation", () => {
      // given
      const email = "test@example.com";
      const bookingReference = "INVALID";

      // when
      service.cancelAppointment(email, bookingReference).subscribe({
        next: () => fail("should have failed"),
        error: (error) => {
          // then
          expect(error.status).toBe(404);
        },
      });

      const req = httpMock.expectOne(
        `${apiUrl}/cancel?email=${email}&bookingReference=${bookingReference}`,
      );
      req.flush(null, { status: 404, statusText: "Not Found" });
    });

    it("should handle already cancelled appointment", () => {
      // given
      const email = "john.doe@example.com";
      const bookingReference = "BR20240115001";

      // when
      service.cancelAppointment(email, bookingReference).subscribe({
        next: () => fail("should have failed"),
        error: (error) => {
          // then
          expect(error.status).toBe(400);
          expect(error.error.message).toBe("Appointment is already cancelled");
        },
      });

      const req = httpMock.expectOne(
        `${apiUrl}/cancel?email=${email}&bookingReference=${bookingReference}`,
      );
      req.flush(
        { message: "Appointment is already cancelled" },
        { status: 400, statusText: "Bad Request" },
      );
    });

    it("should handle past appointment cancellation attempt", () => {
      // given
      const email = "past@example.com";
      const bookingReference = "BR20230101001";

      // when
      service.cancelAppointment(email, bookingReference).subscribe({
        next: () => fail("should have failed"),
        error: (error) => {
          // then
          expect(error.status).toBe(400);
          expect(error.error.message).toBe("Cannot cancel past appointments");
        },
      });

      const req = httpMock.expectOne(
        `${apiUrl}/cancel?email=${email}&bookingReference=${bookingReference}`,
      );
      req.flush(
        { message: "Cannot cancel past appointments" },
        { status: 400, statusText: "Bad Request" },
      );
    });
  });
});
