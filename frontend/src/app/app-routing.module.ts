import { NgModule } from "@angular/core";
import { RouterModule, Routes } from "@angular/router";
import { BookingComponent } from "./components/booking/booking.component";
import { ManageAppointmentComponent } from "./components/manage-appointment/manage-appointment.component";

const routes: Routes = [
  { path: "", redirectTo: "/book", pathMatch: "full" },
  { path: "book", component: BookingComponent },
  { path: "manage", component: ManageAppointmentComponent },
  { path: "**", redirectTo: "/book" },
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule],
})
export class AppRoutingModule {}
