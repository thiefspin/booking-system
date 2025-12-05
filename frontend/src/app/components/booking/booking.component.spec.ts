import {
  ComponentFixture,
  TestBed,
  fakeAsync,
  tick,
  flush,
} from "@angular/core/testing";
import { ReactiveFormsModule } from "@angular/forms";
import { CommonModule } from "@angular/common";
import { of, throwError, Subject } from "rxjs";
import { BookingComponent } from "./booking.component";
import { BranchService } from "../../services/branch.service";
import { AppointmentService } from "../../services/appointment.service";
import { Branch } from "../../models/branch.model";
import {
  TimeSlot,
  Appointment,
  AppointmentStatus,
} from "../../models/appointment.model";
import { DebugElement } from "@angular/core";
import { By } from "@angular/platform-browser";

describe("BookingComponent", () => {
  let component: BookingComponent;
  let fixture: ComponentFixture<BookingComponent>;
  let mockBranchService: jasmine.SpyObj<BranchService>;
  let mockAppointmentService: jasmine.SpyObj<AppointmentService>;

  const mockBranches: Branch[] = [
    {
      id: 1,
      code: "BR001",
      name: "Downtown Branch",
      address: "123 Main St",
      phoneNumber: "555-0100",
      openingTime: "09:00",
      closingTime: "17:00",
      maxConcurrentAppointmentsPerSlot: 3,
    },
    {
      id: 2,
      code: "BR002",
      name: "Uptown Branch",
      address: "456 Oak Ave",
      phoneNumber: "555-0200",
      openingTime: "08:00",
      closingTime: "18:00",
      maxConcurrentAppointmentsPerSlot: 5,
    },
  ];

  const mockTimeSlots: TimeSlot[] = [
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

  beforeEach(() => {
    mockBranchService = jasmine.createSpyObj("BranchService", [
      "getBranches",
      "searchBranches",
      "getBranchById",
    ]);
    mockAppointmentService = jasmine.createSpyObj("AppointmentService", [
      "getAvailableSlots",
      "bookAppointment",
    ]);

    TestBed.configureTestingModule({
      declarations: [BookingComponent],
      imports: [CommonModule, ReactiveFormsModule],
      providers: [
        { provide: BranchService, useValue: mockBranchService },
        { provide: AppointmentService, useValue: mockAppointmentService },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(BookingComponent);
    component = fixture.componentInstance;

    mockBranchService.getBranches.and.returnValue(
      of({
        content: mockBranches,
        totalPages: 1,
        totalElements: 2,
      }),
    );
  });

  describe("Component Initialization", () => {
    it("should create", () => {
      expect(component).toBeTruthy();
    });

    it("should initialize with step 1", () => {
      expect(component.currentStep).toBe(1);
    });

    it("should set min and max dates correctly", () => {
      // given
      const today = new Date();
      const maxDate = new Date();
      maxDate.setDate(maxDate.getDate() + 30);

      // then
      expect(component.minDate).toBe(today.toISOString().split("T")[0]);
      expect(component.maxDate).toBe(maxDate.toISOString().split("T")[0]);
    });

    it("should load branches on init", () => {
      // when
      component.ngOnInit();

      // then
      expect(mockBranchService.getBranches).toHaveBeenCalledWith(0, 6);
      expect(component.branches.length).toBe(2);
      expect(component.filteredBranches).toEqual(mockBranches);
    });

    it("should handle branch loading error", () => {
      // given
      mockBranchService.getBranches.and.returnValue(
        throwError({ status: 500 }),
      );

      // when
      component.ngOnInit();

      // then
      expect(component.error).toBe(
        "Failed to load branches. Please try again.",
      );
      expect(component.loading).toBe(false);
    });
  });

  describe("Form Validation", () => {
    it("should create form with required validators", () => {
      // then
      expect(component.bookingForm.get("firstName")?.hasError("required")).toBe(
        true,
      );
      expect(component.bookingForm.get("lastName")?.hasError("required")).toBe(
        true,
      );
      expect(component.bookingForm.get("email")?.hasError("required")).toBe(
        true,
      );
      expect(
        component.bookingForm.get("phoneNumber")?.hasError("required"),
      ).toBe(true);
      expect(component.bookingForm.get("date")?.hasError("required")).toBe(
        false,
      );
    });

    it("should validate email format", () => {
      // given
      const emailControl = component.bookingForm.get("email");

      // when
      emailControl?.setValue("invalid-email");

      // then
      expect(emailControl?.hasError("email")).toBe(true);

      // when
      emailControl?.setValue("valid@email.com");

      // then
      expect(emailControl?.hasError("email")).toBe(false);
    });

    it("should validate phone number pattern", () => {
      // given
      const phoneControl = component.bookingForm.get("phoneNumber");

      // when
      phoneControl?.setValue("invalid");

      // then
      expect(phoneControl?.hasError("pattern")).toBe(true);

      // when
      phoneControl?.setValue("555-0123");

      // then
      expect(phoneControl?.hasError("pattern")).toBe(false);
    });

    it("should mark field as touched on markFieldAsTouched", () => {
      // given
      const field = component.bookingForm.get("firstName");
      expect(field?.touched).toBe(false);

      // when
      component.markFieldAsTouched("firstName");

      // then
      expect(field?.touched).toBe(true);
    });
  });

  describe("Branch Selection (Step 1)", () => {
    it("should select branch and move to step 2", () => {
      // given
      mockAppointmentService.getAvailableSlots.and.returnValue(
        of(mockTimeSlots),
      );

      // when
      component.selectBranch(mockBranches[0]);

      // then
      expect(component.selectedBranch).toEqual(mockBranches[0]);
      expect(component.currentStep).toBe(2);
    });

    it("should load available slots when branch is selected", () => {
      // given
      mockAppointmentService.getAvailableSlots.and.returnValue(
        of(mockTimeSlots),
      );

      // when
      component.selectBranch(mockBranches[0]);

      // then
      expect(mockAppointmentService.getAvailableSlots).toHaveBeenCalledWith(
        1,
        component.minDate,
      );
    });
  });

  describe("Pagination", () => {
    it("should navigate to next page", () => {
      // given
      component.currentPage = 0;
      component.totalPages = 3;

      // when
      component.nextPage();

      // then
      expect(component.currentPage).toBe(1);
      expect(mockBranchService.getBranches).toHaveBeenCalledWith(1, 6);
    });

    it("should not navigate past last page", () => {
      // given
      component.currentPage = 2;
      component.totalPages = 3;

      // when
      component.nextPage();

      // then
      expect(component.currentPage).toBe(2);
    });

    it("should navigate to previous page", () => {
      // given
      component.currentPage = 2;

      // when
      component.previousPage();

      // then
      expect(component.currentPage).toBe(1);
      expect(mockBranchService.getBranches).toHaveBeenCalledWith(1, 6);
    });

    it("should not navigate before first page", () => {
      // given
      component.currentPage = 0;

      // when
      component.previousPage();

      // then
      expect(component.currentPage).toBe(0);
    });

    it("should go to specific page", () => {
      // given
      component.totalPages = 5;

      // when
      component.goToPage(3);

      // then
      expect(component.currentPage).toBe(3);
      expect(mockBranchService.getBranches).toHaveBeenCalledWith(3, 6);
    });

    it("should change page size and reset to first page", () => {
      // given
      component.currentPage = 2;

      // when
      component.onPageSizeChange(12);

      // then
      expect(component.pageSize).toBe(12);
      expect(component.currentPage).toBe(0);
      expect(mockBranchService.getBranches).toHaveBeenCalledWith(0, 12);
    });

    it("should determine which page numbers to show", () => {
      // given
      component.currentPage = 5;
      component.totalPages = 10;

      // when & then
      expect(component.shouldShowPageNumber(0)).toBe(true);
      expect(component.shouldShowPageNumber(4)).toBe(true);
      expect(component.shouldShowPageNumber(5)).toBe(true);
      expect(component.shouldShowPageNumber(6)).toBe(true);
      expect(component.shouldShowPageNumber(1)).toBe(false);
      expect(component.shouldShowPageNumber(9)).toBe(true);
    });
  });

  describe("Search Functionality", () => {
    it("should perform backend search with debounce", fakeAsync(() => {
      // given
      mockBranchService.searchBranches.and.returnValue(
        of({
          content: [mockBranches[0]],
          totalPages: 1,
          totalElements: 1,
        }),
      );
      component.setupSearchDebounce();

      // when
      component.onSearchChange("downtown");
      tick(300);

      // then
      expect(component.searchTerm).toBe("downtown");
      expect(mockBranchService.searchBranches).toHaveBeenCalledWith(
        "downtown",
        0,
        100,
      );
      expect(component.filteredBranches.length).toBe(1);
    }));

    it("should not search if term is empty", fakeAsync(() => {
      // given
      component.setupSearchDebounce();
      component.branches = mockBranches;
      component.filteredBranches = [];

      // when
      component.onSearchChange("");
      tick(300);

      // then
      expect(component.searchTerm).toBe("");
      expect(component.isSearching).toBe(false);
      expect(component.filteredBranches).toEqual(component.branches);

      // cleanup
      component.ngOnDestroy();
    }));

    it("should clear search", () => {
      // given
      component.searchTerm = "test";
      component.isSearching = true;
      component.searchLoading = true;

      // when
      component.clearSearch();

      // then
      expect(component.searchTerm).toBe("");
      expect(component.isSearching).toBe(false);
      expect(component.searchLoading).toBe(false);
      expect(component.filteredBranches).toEqual(component.branches);
    });

    it("should highlight matching text", () => {
      // given
      const text = "Downtown Branch";
      const searchTerm = "down";

      // when
      const result = component.highlightMatch(text, searchTerm);

      // then
      expect(result).toContain('<mark class="search-highlight">Down</mark>');
    });

    it("should escape special regex characters", () => {
      // given
      const text = "Branch (Main)";
      const searchTerm = "(Main)";

      // when
      const result = component.highlightMatch(text, searchTerm);

      // then
      expect(result).toContain('<mark class="search-highlight">(Main)</mark>');
    });

    it("should handle search error", fakeAsync(() => {
      // given
      mockBranchService.searchBranches.and.returnValue(
        throwError({ status: 500 }),
      );
      component.setupSearchDebounce();

      // when
      component.onSearchChange("test");
      tick(300);

      // then
      expect(component.error).toBe(
        "Failed to search branches. Please try again.",
      );
      expect(component.searchLoading).toBe(false);
    }));
  });

  describe("Time Slot Selection (Step 2)", () => {
    beforeEach(() => {
      mockAppointmentService.getAvailableSlots.and.returnValue(
        of(mockTimeSlots),
      );
      component.selectedBranch = mockBranches[0];
    });

    it("should load slots when date changes", () => {
      // given
      component.bookingForm.get("date")?.setValue("2024-01-16");

      // when
      component.onDateChange();

      // then
      expect(mockAppointmentService.getAvailableSlots).toHaveBeenCalledWith(
        1,
        "2024-01-16",
      );
      expect(component.availableSlots).toEqual(mockTimeSlots);
    });

    it("should select available time slot", () => {
      // given
      const availableSlot = mockTimeSlots[0];

      // when
      component.selectTimeSlot(availableSlot);

      // then
      expect(component.selectedSlot).toEqual(availableSlot);
    });

    it("should not select unavailable slot", () => {
      // given
      const unavailableSlot = mockTimeSlots[2];

      // when
      component.selectTimeSlot(unavailableSlot);

      // then
      expect(component.selectedSlot).toBeNull();
    });

    it("should check if slot is selected", () => {
      // given
      component.selectedSlot = mockTimeSlots[0];

      // when & then
      expect(component.isSlotSelected(mockTimeSlots[0])).toBe(true);
      expect(component.isSlotSelected(mockTimeSlots[1])).toBe(false);
    });

    it("should format time correctly", () => {
      // given
      const timeString = "2024-01-15T14:30:00";

      // when
      const formatted = component.formatTime(timeString);

      // then
      expect(formatted).toMatch(/2:30\s*PM/);
    });

    it("should calculate remaining slots", () => {
      // given
      const slot = mockTimeSlots[0];

      // when
      const remaining = component.getRemainingSlots(slot);

      // then
      expect(remaining).toBe(2);
    });

    it("should enable step 3 when slot selected", () => {
      // given
      component.selectedSlot = mockTimeSlots[0];
      component.bookingForm.get("date")?.setValue("2024-01-15");

      // when
      const canProceed = component.canProceedToStep3();

      // then
      expect(canProceed).toBe(true);
    });
  });

  describe("Booking Submission (Step 3)", () => {
    beforeEach(() => {
      component.selectedBranch = mockBranches[0];
      component.selectedSlot = mockTimeSlots[0];
      component.bookingForm.patchValue({
        date: "2024-01-15",
        firstName: "John",
        lastName: "Doe",
        email: "john.doe@example.com",
        phoneNumber: "555-0123",
        purpose: "Consultation",
        notes: "First visit",
      });
    });

    it("should submit valid booking", () => {
      // given
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
        status: AppointmentStatus.CONFIRMED,
      };
      mockAppointmentService.bookAppointment.and.returnValue(
        of(mockAppointment),
      );

      // when
      component.submitBooking();

      // then
      expect(mockAppointmentService.bookAppointment).toHaveBeenCalled();
      expect(component.confirmedAppointment).toEqual(mockAppointment);
      expect(component.currentStep).toBe(2);
      expect(component.loading).toBe(false);
    });

    it("should not submit invalid form", () => {
      // given
      component.bookingForm.get("email")?.setValue("");

      // when
      component.submitBooking();

      // then
      expect(mockAppointmentService.bookAppointment).not.toHaveBeenCalled();
    });

    it("should handle booking error with message", () => {
      // given
      mockAppointmentService.bookAppointment.and.returnValue(
        throwError({
          status: 400,
          error: { message: "Time slot no longer available" },
        }),
      );

      // when
      component.submitBooking();

      // then
      expect(component.error).toBe("Time slot no longer available");
      expect(component.loading).toBe(false);
    });

    it("should handle 404 branch not found error", () => {
      // given
      mockAppointmentService.bookAppointment.and.returnValue(
        throwError({ status: 404 }),
      );

      // when
      component.submitBooking();

      // then
      expect(component.error).toBe(
        "Branch not found. Please select a different branch.",
      );
    });

    it("should handle generic booking error", () => {
      // given
      mockAppointmentService.bookAppointment.and.returnValue(
        throwError({ status: 500 }),
      );

      // when
      component.submitBooking();

      // then
      expect(component.error).toBe(
        "Failed to book appointment. Please try again.",
      );
    });
  });

  describe("Navigation", () => {
    it("should navigate to next step", () => {
      // given
      component.currentStep = 2;

      // when
      component.nextStep();

      // then
      expect(component.currentStep).toBe(3);
    });

    it("should not navigate beyond step 4", () => {
      // given
      component.currentStep = 4;

      // when
      component.nextStep();

      // then
      expect(component.currentStep).toBe(4);
    });

    it("should navigate to previous step", () => {
      // given
      component.currentStep = 3;

      // when
      component.previousStep();

      // then
      expect(component.currentStep).toBe(2);
    });

    it("should not navigate before step 1", () => {
      // given
      component.currentStep = 1;

      // when
      component.previousStep();

      // then
      expect(component.currentStep).toBe(1);
    });

    it("should clear confirmation when going back from step 4", () => {
      // given
      component.currentStep = 4;
      component.confirmedAppointment = {
        id: 123,
        bookingReference: "BR123",
        branchId: 1,
        customerFirstName: "John",
        customerLastName: "Doe",
        customerEmail: "john@example.com",
        customerPhone: "555-0123",
        appointmentDateTime: "2024-01-15T09:00:00",
        durationMinutes: 30,
        status: AppointmentStatus.CONFIRMED,
      };

      // when
      component.previousStep();

      // then
      expect(component.confirmedAppointment).toBeNull();
    });
  });

  describe("Start New Booking", () => {
    it("should reset all values and return to step 1", () => {
      // given
      component.currentStep = 4;
      component.selectedBranch = mockBranches[0];
      component.selectedSlot = mockTimeSlots[0];
      component.confirmedAppointment = {
        id: 123,
        bookingReference: "BR123",
        branchId: 1,
        customerFirstName: "John",
        customerLastName: "Doe",
        customerEmail: "john@example.com",
        customerPhone: "555-0123",
        appointmentDateTime: "2024-01-15T09:00:00",
        durationMinutes: 30,
        status: AppointmentStatus.CONFIRMED,
      };
      component.error = "Some error";

      // when
      component.startNewBooking();

      // then
      expect(component.currentStep).toBe(1);
      expect(component.selectedBranch).toBeNull();
      expect(component.selectedSlot).toBeNull();
      expect(component.availableSlots).toEqual([]);
      expect(component.confirmedAppointment).toBeNull();
      expect(component.error).toBeNull();
      expect(component.bookingForm.get("date")?.value).toBe(component.minDate);
    });
  });

  describe("Component Cleanup", () => {
    it("should unsubscribe on destroy", () => {
      // given
      spyOn(component["destroy$"], "next");
      spyOn(component["destroy$"], "complete");

      // when
      component.ngOnDestroy();

      // then
      expect(component["destroy$"].next).toHaveBeenCalled();
      expect(component["destroy$"].complete).toHaveBeenCalled();
    });
  });
});
