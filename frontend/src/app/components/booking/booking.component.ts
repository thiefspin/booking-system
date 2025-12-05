import {
  Component,
  OnInit,
  OnDestroy,
  ViewChild,
  ElementRef,
} from "@angular/core";
import { FormBuilder, FormGroup, Validators } from "@angular/forms";
import { BranchService } from "../../services/branch.service";
import { AppointmentService } from "../../services/appointment.service";
import { Branch } from "../../models/branch.model";
import {
  TimeSlot,
  BookingRequest,
  Appointment,
} from "../../models/appointment.model";
import { Subject, debounceTime, distinctUntilChanged } from "rxjs";
import { takeUntil } from "rxjs/operators";

@Component({
  selector: "app-booking",
  templateUrl: "./booking.component.html",
  styleUrls: ["./booking.component.scss"],
})
export class BookingComponent implements OnInit, OnDestroy {
  @ViewChild("searchInput") searchInput!: ElementRef<HTMLInputElement>;
  currentStep: number = 1;
  branches: Branch[] = [];
  filteredBranches: Branch[] = [];
  searchTerm: string = "";
  searchSubject = new Subject<string>();
  private destroy$ = new Subject<void>();
  selectedBranch: Branch | null = null;
  isSearching: boolean = false;
  searchLoading: boolean = false;

  currentPage: number = 0;
  pageSize: number = 6;
  totalPages: number = 0;
  totalElements: number = 0;
  pageSizeOptions: number[] = [6, 12, 24, 48];
  availableSlots: TimeSlot[] = [];
  selectedSlot: TimeSlot | null = null;
  bookingForm: FormGroup;
  minDate: string;
  maxDate: string;
  loading: boolean = false;
  error: string | null = null;
  confirmedAppointment: Appointment | null = null;
  Math = Math;

  constructor(
    private fb: FormBuilder,
    private branchService: BranchService,
    private appointmentService: AppointmentService,
  ) {
    const today = new Date();
    const maxDate = new Date();
    maxDate.setDate(maxDate.getDate() + 30);

    this.minDate = today.toISOString().split("T")[0];
    this.maxDate = maxDate.toISOString().split("T")[0];

    this.bookingForm = this.fb.group({
      date: [this.minDate, Validators.required],
      firstName: ["", [Validators.required, Validators.maxLength(100)]],
      lastName: ["", [Validators.required, Validators.maxLength(100)]],
      email: [
        "",
        [Validators.required, Validators.email, Validators.maxLength(255)],
      ],
      phoneNumber: [
        "",
        [
          Validators.required,
          Validators.pattern("^[\\+]?[0-9\\-\\s\\(\\)]+$"),
          Validators.maxLength(20),
        ],
      ],
      purpose: ["", Validators.maxLength(500)],
      notes: ["", Validators.maxLength(1000)],
    });

    // Mark fields as touched on blur for immediate validation feedback
    this.setupFormValidation();
  }

  ngOnInit(): void {
    this.loadBranches();
    this.setupSearchDebounce();
    this.setupKeyboardShortcuts();
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }

  setupSearchDebounce(): void {
    this.searchSubject
      .pipe(takeUntil(this.destroy$), debounceTime(300), distinctUntilChanged())
      .subscribe((searchValue) => {
        this.searchTerm = searchValue;
        if (searchValue.trim()) {
          this.performBackendSearch(searchValue);
        } else {
          this.clearSearch();
        }
      });
  }

  setupKeyboardShortcuts(): void {
    document.addEventListener("keydown", (event) => {
      if ((event.ctrlKey || event.metaKey) && event.key === "k") {
        event.preventDefault();
        this.focusSearch();
      }
      if (
        event.key === "Escape" &&
        this.searchInput?.nativeElement === document.activeElement
      ) {
        this.clearSearch();
      }
    });
  }

  focusSearch(): void {
    if (this.searchInput?.nativeElement) {
      this.searchInput.nativeElement.focus();
      this.searchInput.nativeElement.select();
    }
  }

  performBackendSearch(query: string): void {
    if (!query || query.trim() === "") {
      this.clearSearch();
      return;
    }

    this.searchLoading = true;
    this.isSearching = true;
    this.error = null;

    this.branchService.searchBranches(query, 0, 100).subscribe({
      next: (response) => {
        this.filteredBranches = response.content || [];
        this.searchLoading = false;
      },
      error: (error) => {
        this.error = "Failed to search branches. Please try again.";
        this.searchLoading = false;
        console.error("Error searching branches:", error);
      },
    });
  }

  loadBranches(): void {
    this.loading = true;
    this.error = null;

    this.branchService.getBranches(this.currentPage, this.pageSize).subscribe({
      next: (response) => {
        this.branches = response.content || [];
        this.filteredBranches = this.branches;
        this.totalPages = response.totalPages || 0;
        this.totalElements = response.totalElements || 0;
        this.loading = false;
      },
      error: (error) => {
        this.error = "Failed to load branches. Please try again.";
        this.loading = false;
        console.error("Error loading branches:", error);
      },
    });
  }

  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages) {
      this.currentPage = page;
      this.loadBranches();
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.loadBranches();
    }
  }

  previousPage(): void {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.loadBranches();
    }
  }

  getPageNumbers(): number[] {
    const pages: number[] = [];
    for (let i = 0; i < this.totalPages; i++) {
      pages.push(i);
    }
    return pages;
  }

  shouldShowPageNumber(pageIndex: number): boolean {
    // Always show first and last page
    if (pageIndex === 0 || pageIndex === this.totalPages - 1) {
      return true;
    }
    // Show pages within 2 of current page
    return Math.abs(pageIndex - this.currentPage) <= 2;
  }

  onPageSizeChange(newSize: string | number): void {
    this.pageSize =
      typeof newSize === "string" ? parseInt(newSize, 10) : newSize;
    this.currentPage = 0;
    this.loadBranches();
  }

  onSearchChange(searchValue: string): void {
    this.searchSubject.next(searchValue);
  }

  clearSearch(): void {
    this.searchTerm = "";
    this.isSearching = false;
    this.searchLoading = false;
    this.filteredBranches = this.branches;
    this.searchSubject.next("");
    if (this.searchInput?.nativeElement) {
      this.searchInput.nativeElement.blur();
    }
  }

  highlightMatch(text: string, searchTerm: string): string {
    if (!searchTerm || searchTerm.trim() === "") {
      return text;
    }

    const searchRegex = new RegExp(`(${this.escapeRegExp(searchTerm)})`, "gi");
    return text.replace(
      searchRegex,
      '<mark class="search-highlight">$1</mark>',
    );
  }

  private escapeRegExp(text: string): string {
    return text.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  }

  selectBranch(branch: Branch): void {
    this.selectedBranch = branch;
    this.nextStep();
    this.loadAvailableSlots();
  }

  onDateChange(): void {
    if (this.selectedBranch && this.bookingForm.get("date")?.value) {
      this.loadAvailableSlots();
    }
  }

  loadAvailableSlots(): void {
    if (!this.selectedBranch || !this.bookingForm.get("date")?.value) {
      return;
    }

    this.loading = true;
    this.error = null;
    this.selectedSlot = null;

    const selectedDate = this.bookingForm.get("date")?.value;

    this.appointmentService
      .getAvailableSlots(this.selectedBranch.id, selectedDate)
      .subscribe({
        next: (slots) => {
          this.availableSlots = slots;
          this.loading = false;
        },
        error: (error) => {
          this.error = "Failed to load available time slots. Please try again.";
          this.loading = false;
          console.error("Error loading slots:", error);
        },
      });
  }

  selectTimeSlot(slot: TimeSlot): void {
    if (slot.available && slot.currentBookings < slot.maxBookings) {
      this.selectedSlot = slot;
    }
  }

  isSlotSelected(slot: TimeSlot): boolean {
    return this.selectedSlot?.startTime === slot.startTime;
  }

  formatTime(timeString: string): string {
    const date = new Date(timeString);
    return date.toLocaleTimeString("en-US", {
      hour: "numeric",
      minute: "2-digit",
      hour12: true,
    });
  }

  nextStep(): void {
    if (this.currentStep < 4) {
      this.currentStep++;
    }
  }

  previousStep(): void {
    if (this.currentStep > 1) {
      this.currentStep--;
      if (this.currentStep < 4) {
        this.confirmedAppointment = null;
      }
    }
  }

  canProceedToStep3(): boolean {
    return (
      this.selectedSlot !== null && this.bookingForm.get("date")?.valid === true
    );
  }

  submitBooking(): void {
    if (!this.bookingForm.valid || !this.selectedBranch || !this.selectedSlot) {
      return;
    }

    this.loading = true;
    this.error = null;

    const appointmentDateTime = this.selectedSlot.startTime;

    const bookingRequest: BookingRequest = {
      branchId: this.selectedBranch.id,
      firstName: this.bookingForm.get("firstName")?.value,
      lastName: this.bookingForm.get("lastName")?.value,
      email: this.bookingForm.get("email")?.value,
      phoneNumber: this.bookingForm.get("phoneNumber")?.value,
      appointmentDateTime: appointmentDateTime,
      durationMinutes: 30,
      purpose: this.bookingForm.get("purpose")?.value || undefined,
      notes: this.bookingForm.get("notes")?.value || undefined,
    };

    this.appointmentService.bookAppointment(bookingRequest).subscribe({
      next: (appointment) => {
        this.confirmedAppointment = appointment;
        this.loading = false;
        this.nextStep();
      },
      error: (error) => {
        this.loading = false;
        if (error.error?.message) {
          this.error = error.error.message;
        } else if (error.status === 400) {
          this.error =
            "Invalid booking information. Please check your details and try again.";
        } else if (error.status === 404) {
          this.error = "Branch not found. Please select a different branch.";
        } else {
          this.error = "Failed to book appointment. Please try again.";
        }
        console.error("Error booking appointment:", error);
      },
    });
  }

  startNewBooking(): void {
    this.currentStep = 1;
    this.selectedBranch = null;
    this.selectedSlot = null;
    this.availableSlots = [];
    this.confirmedAppointment = null;
    this.bookingForm.reset({
      date: this.minDate,
    });
    this.error = null;
  }

  getRemainingSlots(slot: TimeSlot): number {
    return slot.maxBookings - slot.currentBookings;
  }

  setupFormValidation(): void {
    // This will be called from the template on blur events
    // No need to set up listeners here as we'll use template events
  }

  markFieldAsTouched(fieldName: string): void {
    const field = this.bookingForm.get(fieldName);
    if (field) {
      field.markAsTouched();
      field.updateValueAndValidity();
    }
  }
}
