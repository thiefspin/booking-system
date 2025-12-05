import { ComponentFixture, TestBed } from "@angular/core/testing";
import { ReactiveFormsModule } from "@angular/forms";
import { of, throwError } from "rxjs";
import { ManageAppointmentComponent } from "./manage-appointment.component";
import { AppointmentService } from "../../services/appointment.service";
import { Appointment, AppointmentStatus } from "../../models/appointment.model";

describe("ManageAppointmentComponent", () => {
  let component: ManageAppointmentComponent;
  let fixture: ComponentFixture<ManageAppointmentComponent>;
  let mockAppointmentService: jasmine.SpyObj<AppointmentService>;

  const mockAppointment: Appointment = {
    id: 123,
    bookingReference: "BR20240115001",
    branchId: 1,
    customerFirstName: "John",
    customerLastName: "Doe",
    customerEmail: "john.doe@example.com",
    customerPhone: "555-0123",
    appointmentDateTime: "2024-12-15T09:00:00",
    durationMinutes: 30,
    purpose: "Consultation",
    notes: "First visit",
    status: AppointmentStatus.CONFIRMED,
  };

  beforeEach(() => {
    mockAppointmentService = jasmine.createSpyObj("AppointmentService", [
      "lookupAppointment",
      "cancelAppointment",
    ]);

    TestBed.configureTestingModule({
      declarations: [ManageAppointmentComponent],
      imports: [ReactiveFormsModule],
      providers: [
        { provide: AppointmentService, useValue: mockAppointmentService },
      ],
    });

    fixture = TestBed.createComponent(ManageAppointmentComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe("Component Initialization", () => {
    it("should create", () => {
      // then
      expect(component).toBeTruthy();
    });

    it("should initialize lookup form with validators", () => {
      // when
      const form = component.lookupForm;

      // then
      expect(form.get("email")?.hasError("required")).toBe(true);
      expect(form.get("bookingReference")?.hasError("required")).toBe(true);
    });

    it("should initialize cancel form", () => {
      // when
      const form = component.cancelForm;

      // then
      expect(form.get("reason")).toBeDefined();
      expect(form.get("reason")?.value).toBe("");
    });

    it("should initialize with null appointment", () => {
      // then
      expect(component.appointment).toBeNull();
      expect(component.loading).toBe(false);
      expect(component.error).toBeNull();
      expect(component.successMessage).toBeNull();
      expect(component.showCancelDialog).toBe(false);
    });
  });

  describe("Form Validation", () => {
    it("should validate email format", () => {
      // given
      const emailControl = component.lookupForm.get("email");

      // when
      emailControl?.setValue("invalid-email");

      // then
      expect(emailControl?.hasError("email")).toBe(true);

      // when
      emailControl?.setValue("valid@email.com");

      // then
      expect(emailControl?.hasError("email")).toBe(false);
    });

    it("should require booking reference", () => {
      // given
      const referenceControl = component.lookupForm.get("bookingReference");

      // when
      referenceControl?.setValue("");

      // then
      expect(referenceControl?.hasError("required")).toBe(true);

      // when
      referenceControl?.setValue("BR123");

      // then
      expect(referenceControl?.hasError("required")).toBe(false);
    });

    it("should mark field as touched", () => {
      // given
      const field = component.lookupForm.get("email");
      expect(field?.touched).toBe(false);

      // when
      component.markFieldAsTouched("email");

      // then
      expect(field?.touched).toBe(true);
    });
  });

  describe("Appointment Lookup", () => {
    it("should lookup appointment successfully", () => {
      // given
      component.lookupForm.setValue({
        email: "john.doe@example.com",
        bookingReference: "BR20240115001",
      });
      mockAppointmentService.lookupAppointment.and.returnValue(
        of(mockAppointment),
      );

      // when
      component.lookupAppointment();

      // then
      expect(mockAppointmentService.lookupAppointment).toHaveBeenCalledWith(
        "john.doe@example.com",
        "BR20240115001",
      );
      expect(component.appointment).toEqual(mockAppointment);
      expect(component.loading).toBe(false);
      expect(component.error).toBeNull();
    });

    it("should not lookup with invalid form", () => {
      // given
      component.lookupForm.setValue({
        email: "",
        bookingReference: "",
      });

      // when
      component.lookupAppointment();

      // then
      expect(mockAppointmentService.lookupAppointment).not.toHaveBeenCalled();
    });

    it("should handle appointment not found error", () => {
      // given
      component.lookupForm.setValue({
        email: "test@example.com",
        bookingReference: "INVALID",
      });
      mockAppointmentService.lookupAppointment.and.returnValue(
        throwError({ status: 404 }),
      );

      // when
      component.lookupAppointment();

      // then
      expect(component.error).toBe(
        "Appointment not found. Please check your email and booking reference.",
      );
      expect(component.appointment).toBeNull();
      expect(component.loading).toBe(false);
    });

    it("should handle generic lookup error", () => {
      // given
      component.lookupForm.setValue({
        email: "test@example.com",
        bookingReference: "BR123",
      });
      mockAppointmentService.lookupAppointment.and.returnValue(
        throwError({ status: 500 }),
      );

      // when
      component.lookupAppointment();

      // then
      expect(component.error).toBe(
        "An error occurred while looking up your appointment. Please try again.",
      );
      expect(component.loading).toBe(false);
    });

    it("should clear previous states on new lookup", () => {
      // given
      component.appointment = mockAppointment;
      component.error = "Previous error";
      component.successMessage = "Previous success";
      component.lookupForm.setValue({
        email: "new@example.com",
        bookingReference: "NEW123",
      });
      mockAppointmentService.lookupAppointment.and.returnValue(
        of(mockAppointment),
      );

      // when
      component.lookupAppointment();

      // then
      expect(component.error).toBeNull();
      expect(component.successMessage).toBeNull();
    });
  });

  describe("Cancel Dialog", () => {
    it("should show cancel confirmation dialog", () => {
      // given
      component.error = "Some error";
      component.successMessage = "Some message";

      // when
      component.showCancelConfirmation();

      // then
      expect(component.showCancelDialog).toBe(true);
      expect(component.error).toBeNull();
      expect(component.successMessage).toBeNull();
    });

    it("should close cancel dialog and reset form", () => {
      // given
      component.showCancelDialog = true;
      component.cancelForm.setValue({ reason: "Test reason" });

      // when
      component.closeCancelDialog();

      // then
      expect(component.showCancelDialog).toBe(false);
      expect(component.cancelForm.get("reason")?.value).toBeNull();
    });
  });

  describe("Appointment Cancellation", () => {
    beforeEach(() => {
      component.appointment = mockAppointment;
      component.lookupForm.setValue({
        email: "john.doe@example.com",
        bookingReference: "BR20240115001",
      });
    });

    it("should cancel appointment with reason", () => {
      // given
      const cancelledAppointment = {
        ...mockAppointment,
        status: AppointmentStatus.CANCELLED,
      };
      component.cancelForm.setValue({ reason: "Cannot attend" });
      mockAppointmentService.cancelAppointment.and.returnValue(
        of(cancelledAppointment),
      );

      // when
      component.cancelAppointment();

      // then
      expect(mockAppointmentService.cancelAppointment).toHaveBeenCalledWith(
        "john.doe@example.com",
        "BR20240115001",
        "Cannot attend",
      );
      expect(component.appointment).toEqual(cancelledAppointment);
      expect(component.successMessage).toBe(
        "Your appointment has been successfully cancelled.",
      );
      expect(component.showCancelDialog).toBe(false);
      expect(component.loading).toBe(false);
    });

    it("should cancel appointment with default reason", () => {
      // given
      const cancelledAppointment = {
        ...mockAppointment,
        status: AppointmentStatus.CANCELLED,
      };
      component.cancelForm.setValue({ reason: "" });
      mockAppointmentService.cancelAppointment.and.returnValue(
        of(cancelledAppointment),
      );

      // when
      component.cancelAppointment();

      // then
      expect(mockAppointmentService.cancelAppointment).toHaveBeenCalledWith(
        "john.doe@example.com",
        "BR20240115001",
        "Customer requested cancellation",
      );
    });

    it("should not cancel if no appointment", () => {
      // given
      component.appointment = null;

      // when
      component.cancelAppointment();

      // then
      expect(mockAppointmentService.cancelAppointment).not.toHaveBeenCalled();
    });

    it("should handle appointment not found on cancellation", () => {
      // given
      mockAppointmentService.cancelAppointment.and.returnValue(
        throwError({ status: 404 }),
      );

      // when
      component.cancelAppointment();

      // then
      expect(component.error).toBe("Appointment not found.");
      expect(component.loading).toBe(false);
    });

    it("should handle already cancelled appointment", () => {
      // given
      mockAppointmentService.cancelAppointment.and.returnValue(
        throwError({
          status: 400,
          error: { message: "Appointment already cancelled" },
        }),
      );

      // when
      component.cancelAppointment();

      // then
      expect(component.error).toBe("Appointment already cancelled");
    });

    it("should handle cancellation error without message", () => {
      // given
      mockAppointmentService.cancelAppointment.and.returnValue(
        throwError({ status: 400 }),
      );

      // when
      component.cancelAppointment();

      // then
      expect(component.error).toBe(
        "Cannot cancel this appointment. It may already be cancelled or completed.",
      );
    });

    it("should handle generic cancellation error", () => {
      // given
      mockAppointmentService.cancelAppointment.and.returnValue(
        throwError({ status: 500 }),
      );

      // when
      component.cancelAppointment();

      // then
      expect(component.error).toBe(
        "An error occurred while cancelling your appointment. Please try again.",
      );
    });
  });

  describe("Cancellation Eligibility", () => {
    it("should allow cancellation for confirmed appointment", () => {
      // given
      const futureDate = new Date();
      futureDate.setFullYear(futureDate.getFullYear() + 1);
      component.appointment = {
        ...mockAppointment,
        status: AppointmentStatus.CONFIRMED,
        appointmentDateTime: futureDate.toISOString(),
      };

      // when
      const canCancel = component.canCancelAppointment();

      // then
      expect(canCancel).toBe(true);
    });

    it("should allow cancellation for pending appointment", () => {
      // given
      const futureDate = new Date();
      futureDate.setFullYear(futureDate.getFullYear() + 1);
      component.appointment = {
        ...mockAppointment,
        status: AppointmentStatus.PENDING,
        appointmentDateTime: futureDate.toISOString(),
      };

      // when
      const canCancel = component.canCancelAppointment();

      // then
      expect(canCancel).toBe(true);
    });

    it("should not allow cancellation for cancelled appointment", () => {
      // given
      component.appointment = {
        ...mockAppointment,
        status: AppointmentStatus.CANCELLED,
      };

      // when
      const canCancel = component.canCancelAppointment();

      // then
      expect(canCancel).toBe(false);
    });

    it("should not allow cancellation for completed appointment", () => {
      // given
      component.appointment = {
        ...mockAppointment,
        status: AppointmentStatus.COMPLETED,
      };

      // when
      const canCancel = component.canCancelAppointment();

      // then
      expect(canCancel).toBe(false);
    });

    it("should not allow cancellation for no-show appointment", () => {
      // given
      component.appointment = {
        ...mockAppointment,
        status: AppointmentStatus.NO_SHOW,
      };

      // when
      const canCancel = component.canCancelAppointment();

      // then
      expect(canCancel).toBe(false);
    });

    it("should not allow cancellation for past appointment", () => {
      // given
      const pastDate = new Date();
      pastDate.setDate(pastDate.getDate() - 1);
      component.appointment = {
        ...mockAppointment,
        status: AppointmentStatus.CONFIRMED,
        appointmentDateTime: pastDate.toISOString(),
      };

      // when
      const canCancel = component.canCancelAppointment();

      // then
      expect(canCancel).toBe(false);
    });

    it("should return false if no appointment", () => {
      // given
      component.appointment = null;

      // when
      const canCancel = component.canCancelAppointment();

      // then
      expect(canCancel).toBe(false);
    });
  });

  describe("Date/Time Formatting", () => {
    it("should format date and time correctly", () => {
      // given
      const dateTime = "2024-01-15T14:30:00";

      // when
      const formatted = component.formatDateTime(dateTime);

      // then
      expect(formatted).toContain("January");
      expect(formatted).toContain("15");
      expect(formatted).toContain("2024");
      expect(formatted).toMatch(/2:30\s*PM/);
    });
  });

  describe("Status Color", () => {
    it("should return success for CONFIRMED status", () => {
      // given
      // when
      const color = component.getStatusColor("CONFIRMED");
      // then
      expect(color).toBe("success");
    });

    it("should return success for PENDING status", () => {
      // given
      // when
      const color = component.getStatusColor("PENDING");
      // then
      expect(color).toBe("success");
    });

    it("should return danger for CANCELLED status", () => {
      // given
      // when
      const color = component.getStatusColor("CANCELLED");
      // then
      expect(color).toBe("danger");
    });

    it("should return secondary for COMPLETED status", () => {
      // given
      // when
      const color = component.getStatusColor("COMPLETED");
      // then
      expect(color).toBe("secondary");
    });

    it("should return warning for NO_SHOW status", () => {
      // given
      // when
      const color = component.getStatusColor("NO_SHOW");
      // then
      expect(color).toBe("warning");
    });

    it("should return info for unknown status", () => {
      // given
      // when
      const color = component.getStatusColor("UNKNOWN");
      // then
      expect(color).toBe("info");
    });
  });

  describe("Reset Form", () => {
    it("should reset all component state", () => {
      // given
      component.appointment = mockAppointment;
      component.error = "Test error";
      component.successMessage = "Test success";
      component.showCancelDialog = true;
      component.lookupForm.setValue({
        email: "test@example.com",
        bookingReference: "BR123",
      });
      component.cancelForm.setValue({
        reason: "Test reason",
      });

      // when
      component.resetForm();

      // then
      expect(component.appointment).toBeNull();
      expect(component.error).toBeNull();
      expect(component.successMessage).toBeNull();
      expect(component.showCancelDialog).toBe(false);
      expect(component.lookupForm.get("email")?.value).toBeNull();
      expect(component.lookupForm.get("bookingReference")?.value).toBeNull();
      expect(component.cancelForm.get("reason")?.value).toBeNull();
    });
  });
});
