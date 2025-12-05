import { TestBed } from "@angular/core/testing";
import { RouterTestingModule } from "@angular/router/testing";
import { AppComponent } from "./app.component";

describe("AppComponent", () => {
  beforeEach(() =>
    TestBed.configureTestingModule({
      imports: [RouterTestingModule],
      declarations: [AppComponent],
    }),
  );

  it("should create the app", () => {
    // when
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    // then
    expect(app).toBeTruthy();
  });

  it(`should have as title 'web-ui'`, () => {
    // when
    const fixture = TestBed.createComponent(AppComponent);
    const app = fixture.componentInstance;
    // then
    expect(app.title).toEqual("web-ui");
  });

  it("should render navigation bar", () => {
    // given
    const fixture = TestBed.createComponent(AppComponent);
    // when
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    // then
    expect(compiled.querySelector(".navbar")).toBeTruthy();
    expect(compiled.querySelector(".navbar-brand")?.textContent).toContain(
      "Appointment Booking System",
    );
  });

  it("should render navigation links", () => {
    // given
    const fixture = TestBed.createComponent(AppComponent);
    // when
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    const navLinks = compiled.querySelectorAll(".nav-link");
    // then
    expect(navLinks.length).toBe(2);
    expect(navLinks[0].textContent).toContain("Book Appointment");
    expect(navLinks[1].textContent).toContain("Manage Booking");
  });

  it("should have router outlet", () => {
    // given
    const fixture = TestBed.createComponent(AppComponent);
    // when
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    // then
    expect(compiled.querySelector("router-outlet")).toBeTruthy();
  });

  it("should render footer", () => {
    // given
    const fixture = TestBed.createComponent(AppComponent);
    // when
    fixture.detectChanges();
    const compiled = fixture.nativeElement as HTMLElement;
    // then
    expect(compiled.querySelector("footer")).toBeTruthy();
    expect(compiled.querySelector("footer")?.textContent).toContain(
      "Appointment Booking System",
    );
    expect(compiled.querySelector("footer")?.textContent).toContain(
      "Support: 1-800-BOOKING",
    );
  });
});
