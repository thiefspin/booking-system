import { Component } from "@angular/core";
import { FormBuilder, FormGroup, Validators } from "@angular/forms";
import { AppointmentService } from "../../services/appointment.service";
import { Appointment } from "../../models/appointment.model";

@Component({
  selector: "app-manage-appointment",
  templateUrl: "./manage-appointment.component.html",
  styleUrls: ["./manage-appointment.component.scss"],
})
export class ManageAppointmentComponent {
  lookupForm: FormGroup;
  cancelForm: FormGroup;
  appointment: Appointment | null = null;
  loading: boolean = false;
  error: string | null = null;
  successMessage: string | null = null;
  showCancelDialog: boolean = false;

  constructor(
    private fb: FormBuilder,
    private appointmentService: AppointmentService,
  ) {
    this.lookupForm = this.fb.group({
      email: ["", [Validators.required, Validators.email]],
      bookingReference: ["", [Validators.required]],
    });

    this.cancelForm = this.fb.group({
      reason: [""],
    });
  }

  lookupAppointment(): void {
    if (!this.lookupForm.valid) {
      return;
    }

    this.loading = true;
    this.error = null;
    this.successMessage = null;
    this.appointment = null;

    const email = this.lookupForm.get("email")?.value;
    const bookingReference = this.lookupForm.get("bookingReference")?.value;

    this.appointmentService
      .lookupAppointment(email, bookingReference)
      .subscribe({
        next: (appointment) => {
          this.appointment = appointment;
          this.loading = false;
        },
        error: (error) => {
          this.loading = false;
          if (error.status === 404) {
            this.error =
              "Appointment not found. Please check your email and booking reference.";
          } else {
            this.error =
              "An error occurred while looking up your appointment. Please try again.";
          }
          console.error("Error looking up appointment:", error);
        },
      });
  }

  showCancelConfirmation(): void {
    this.showCancelDialog = true;
    this.error = null;
    this.successMessage = null;
  }

  closeCancelDialog(): void {
    this.showCancelDialog = false;
    this.cancelForm.reset();
  }

  cancelAppointment(): void {
    if (!this.appointment) {
      return;
    }

    this.loading = true;
    this.error = null;
    this.successMessage = null;

    const email = this.lookupForm.get("email")?.value;
    const bookingReference = this.appointment.bookingReference;
    const reason =
      this.cancelForm.get("reason")?.value || "Customer requested cancellation";

    this.appointmentService
      .cancelAppointment(email, bookingReference, reason)
      .subscribe({
        next: (cancelledAppointment) => {
          this.appointment = cancelledAppointment;
          this.loading = false;
          this.showCancelDialog = false;
          this.successMessage =
            "Your appointment has been successfully cancelled.";
          this.cancelForm.reset();
        },
        error: (error) => {
          this.loading = false;
          if (error.status === 404) {
            this.error = "Appointment not found.";
          } else if (error.status === 400) {
            this.error =
              error.error?.message ||
              "Cannot cancel this appointment. It may already be cancelled or completed.";
          } else {
            this.error =
              "An error occurred while cancelling your appointment. Please try again.";
          }
          console.error("Error cancelling appointment:", error);
        },
      });
  }

  formatDateTime(dateTime: string): string {
    const date = new Date(dateTime);
    return date.toLocaleString("en-US", {
      weekday: "long",
      year: "numeric",
      month: "long",
      day: "numeric",
      hour: "numeric",
      minute: "2-digit",
      hour12: true,
    });
  }

  getStatusColor(status: string): string {
    switch (status) {
      case "CONFIRMED":
      case "PENDING":
        return "success";
      case "CANCELLED":
        return "danger";
      case "COMPLETED":
        return "secondary";
      case "NO_SHOW":
        return "warning";
      default:
        return "info";
    }
  }

  canCancelAppointment(): boolean {
    if (!this.appointment) {
      return false;
    }

    if (
      this.appointment.status !== "PENDING" &&
      this.appointment.status !== "CONFIRMED"
    ) {
      return false;
    }

    const appointmentDate = new Date(this.appointment.appointmentDateTime);
    const now = new Date();
    return appointmentDate > now;
  }

  resetForm(): void {
    this.lookupForm.reset();
    this.cancelForm.reset();
    this.appointment = null;
    this.error = null;
    this.successMessage = null;
    this.showCancelDialog = false;
  }

  markFieldAsTouched(fieldName: string): void {
    const field = this.lookupForm.get(fieldName);
    if (field) {
      field.markAsTouched();
      field.updateValueAndValidity();
    }
  }
}
