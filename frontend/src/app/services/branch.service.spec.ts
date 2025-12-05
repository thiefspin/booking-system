import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { BranchService } from './branch.service';
import { Branch } from '../models/branch.model';
import { environment } from '../../environments/environment';

describe('BranchService', () => {
  let service: BranchService;
  let httpMock: HttpTestingController;
  const apiUrl = `${environment.apiUrl}/api/branches`;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [BranchService]
    });
    service = TestBed.inject(BranchService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  describe('getBranches', () => {
    it('should retrieve branches with default pagination', () => {
      // given
      const mockResponse = {
        content: [
          {
            id: 1,
            code: 'BR001',
            name: 'Downtown Branch',
            address: '123 Main St',
            phoneNumber: '555-0100',
            openingTime: '09:00',
            closingTime: '17:00',
            maxConcurrentAppointmentsPerSlot: 3
          },
          {
            id: 2,
            code: 'BR002',
            name: 'Uptown Branch',
            address: '456 Oak Ave',
            phoneNumber: '555-0200',
            openingTime: '08:00',
            closingTime: '18:00',
            maxConcurrentAppointmentsPerSlot: 5
          }
        ],
        totalPages: 1,
        totalElements: 2
      };

      // when
      service.getBranches().subscribe(response => {
        // then
        expect(response).toEqual(mockResponse);
        expect(response.content.length).toBe(2);
      });

      const req = httpMock.expectOne(`${apiUrl}?page=0&size=100`);
      expect(req.request.method).toBe('GET');
      req.flush(mockResponse);
    });

    it('should retrieve branches with custom pagination', () => {
      // given
      const mockResponse = {
        content: [],
        totalPages: 5,
        totalElements: 50
      };
      const page = 2;
      const size = 10;

      // when
      service.getBranches(page, size).subscribe(response => {
        // then
        expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(`${apiUrl}?page=${page}&size=${size}`);
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('page')).toBe(page.toString());
      expect(req.request.params.get('size')).toBe(size.toString());
      req.flush(mockResponse);
    });

    it('should handle empty response', () => {
      // given
      const mockResponse = {
        content: [],
        totalPages: 0,
        totalElements: 0
      };

      // when
      service.getBranches(0, 10).subscribe(response => {
        // then
        expect(response.content).toEqual([]);
        expect(response.totalElements).toBe(0);
      });

      const req = httpMock.expectOne(`${apiUrl}?page=0&size=10`);
      req.flush(mockResponse);
    });

    it('should handle HTTP error', () => {
      // given
      const errorMessage = 'Server error';

      // when
      service.getBranches().subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          // then
          expect(error.status).toBe(500);
          expect(error.statusText).toBe(errorMessage);
        }
      });

      const req = httpMock.expectOne(`${apiUrl}?page=0&size=100`);
      req.flush(null, { status: 500, statusText: errorMessage });
    });
  });

  describe('searchBranches', () => {
    it('should search branches with query', () => {
      // given
      const searchQuery = 'downtown';
      const mockResponse = {
        content: [
          {
            id: 1,
            code: 'BR001',
            name: 'Downtown Branch',
            address: '123 Main St',
            phoneNumber: '555-0100',
            openingTime: '09:00',
            closingTime: '17:00',
            maxConcurrentAppointmentsPerSlot: 3
          }
        ],
        totalPages: 1,
        totalElements: 1
      };

      // when
      service.searchBranches(searchQuery).subscribe(response => {
        // then
        expect(response).toEqual(mockResponse);
        expect(response.content.length).toBe(1);
      });

      const req = httpMock.expectOne(`${apiUrl}/search?query=${searchQuery}&page=0&size=100`);
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('query')).toBe(searchQuery);
      req.flush(mockResponse);
    });

    it('should search branches with custom pagination', () => {
      // given
      const searchQuery = 'branch';
      const page = 1;
      const size = 5;
      const mockResponse = {
        content: [],
        totalPages: 2,
        totalElements: 10
      };

      // when
      service.searchBranches(searchQuery, page, size).subscribe(response => {
        // then
        expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(`${apiUrl}/search?query=${searchQuery}&page=${page}&size=${size}`);
      expect(req.request.params.get('query')).toBe(searchQuery);
      expect(req.request.params.get('page')).toBe(page.toString());
      expect(req.request.params.get('size')).toBe(size.toString());
      req.flush(mockResponse);
    });

    it('should handle empty search results', () => {
      // given
      const searchQuery = 'nonexistent';
      const mockResponse = {
        content: [],
        totalPages: 0,
        totalElements: 0
      };

      // when
      service.searchBranches(searchQuery).subscribe(response => {
        // then
        expect(response.content).toEqual([]);
        expect(response.totalElements).toBe(0);
      });

      const req = httpMock.expectOne(`${apiUrl}/search?query=${searchQuery}&page=0&size=100`);
      req.flush(mockResponse);
    });

    it('should handle special characters in search query', () => {
      // given
      const searchQuery = 'V&A Museum';
      const mockResponse = {
        content: [],
        totalPages: 0,
        totalElements: 0
      };

      // when
      service.searchBranches(searchQuery).subscribe(response => {
        // then
        expect(response).toEqual(mockResponse);
      });

      const req = httpMock.expectOne(req => req.url.includes(`${apiUrl}/search`));
      expect(req.request.params.get('query')).toBe(searchQuery);
      req.flush(mockResponse);
    });

    it('should handle search error', () => {
      // given
      const searchQuery = 'test';
      const errorMessage = 'Search failed';

      // when
      service.searchBranches(searchQuery).subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          // then
          expect(error.status).toBe(400);
          expect(error.statusText).toBe(errorMessage);
        }
      });

      const req = httpMock.expectOne(`${apiUrl}/search?query=${searchQuery}&page=0&size=100`);
      req.flush(null, { status: 400, statusText: errorMessage });
    });
  });

  describe('getBranchById', () => {
    it('should retrieve branch by id', () => {
      // given
      const branchId = 1;
      const mockBranch: Branch = {
        id: branchId,
        code: 'BR001',
        name: 'Downtown Branch',
        address: '123 Main St',
        phoneNumber: '555-0100',
        openingTime: '09:00',
        closingTime: '17:00',
        maxConcurrentAppointmentsPerSlot: 3
      };

      // when
      service.getBranchById(branchId).subscribe(branch => {
        // then
        expect(branch).toEqual(mockBranch);
        expect(branch.id).toBe(branchId);
      });

      const req = httpMock.expectOne(`${apiUrl}/${branchId}`);
      expect(req.request.method).toBe('GET');
      req.flush(mockBranch);
    });

    it('should handle branch not found', () => {
      // given
      const branchId = 999;

      // when
      service.getBranchById(branchId).subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          // then
          expect(error.status).toBe(404);
          expect(error.statusText).toBe('Not Found');
        }
      });

      const req = httpMock.expectOne(`${apiUrl}/${branchId}`);
      req.flush(null, { status: 404, statusText: 'Not Found' });
    });

    it('should handle server error when getting branch', () => {
      // given
      const branchId = 1;

      // when
      service.getBranchById(branchId).subscribe({
        next: () => fail('should have failed'),
        error: (error) => {
          // then
          expect(error.status).toBe(500);
        }
      });

      const req = httpMock.expectOne(`${apiUrl}/${branchId}`);
      req.flush(null, { status: 500, statusText: 'Internal Server Error' });
    });
  });
});
