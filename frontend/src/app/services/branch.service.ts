import { Injectable } from "@angular/core";
import { HttpClient, HttpParams } from "@angular/common/http";
import { Observable } from "rxjs";
import { Branch } from "../models/branch.model";
import { environment } from "../../environments/environment";

@Injectable({
  providedIn: "root",
})
export class BranchService {
  private apiUrl = `${environment.apiUrl}/api/branches`;

  constructor(private http: HttpClient) {}

  getBranches(page: number = 0, size: number = 100): Observable<any> {
    const params = new HttpParams()
      .set("page", page.toString())
      .set("size", size.toString());

    return this.http.get<any>(this.apiUrl, { params });
  }

  searchBranches(
    query: string,
    page: number = 0,
    size: number = 100,
  ): Observable<any> {
    const params = new HttpParams()
      .set("query", query)
      .set("page", page.toString())
      .set("size", size.toString());

    return this.http.get<any>(`${this.apiUrl}/search`, { params });
  }

  getBranchById(id: number): Observable<Branch> {
    return this.http.get<Branch>(`${this.apiUrl}/${id}`);
  }
}
