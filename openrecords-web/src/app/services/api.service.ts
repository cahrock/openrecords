import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  CreateFoiaRequest,
  FoiaRequest,
  PageResponse,
} from '../models/foia-request.model';

export interface HealthResponse {
  status: string;
  service: string;
  timestamp: string;
}

export interface PageQuery {
  page?: number;
  size?: number;
  sort?: string;  // e.g. "createdAt,desc"
}

@Injectable({
  providedIn: 'root',
})
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = '/api/v1';

  // ==============================
  // Health
  // ==============================

  getHealth(): Observable<HealthResponse> {
    return this.http.get<HealthResponse>(`${this.baseUrl}/health`);
  }

  // ==============================
  // FOIA Requests
  // ==============================

  /**
   * List all requests with pagination.
   * Default sort: newest first.
   */
  /**
   * List requests with optional filters.
   * Default sort: newest first.
   */
  listRequests(query: PageQuery & {
    status?: string;
    assigneeId?: number;
    unassignedOnly?: boolean;
    search?: string;
  } = {}): Observable<PageResponse<FoiaRequest>> {
    let params = new HttpParams();
    params = params.set('page', String(query.page ?? 0));
    params = params.set('size', String(query.size ?? 20));
    params = params.set('sort', query.sort ?? 'createdAt,desc');

    if (query.status) params = params.set('status', query.status);
    if (query.assigneeId != null) params = params.set('assigneeId', String(query.assigneeId));
    if (query.unassignedOnly) params = params.set('unassignedOnly', 'true');
    if (query.search) params = params.set('search', query.search);

    return this.http.get<PageResponse<FoiaRequest>>(
      `${this.baseUrl}/requests`,
      { params }
    );
  }

  /**
   * Fetch a single request by its UUID.
   */
  getRequestById(id: string): Observable<FoiaRequest> {
    return this.http.get<FoiaRequest>(`${this.baseUrl}/requests/${id}`);
  }

  /**
   * Fetch a single request by its human-readable tracking number.
   */
  getRequestByTrackingNumber(trackingNumber: string): Observable<FoiaRequest> {
    return this.http.get<FoiaRequest>(
      `${this.baseUrl}/requests/tracking/${trackingNumber}`
    );
  }

  /**
   * Create a new FOIA request.
   */
  createRequest(request: CreateFoiaRequest): Observable<FoiaRequest> {
    return this.http.post<FoiaRequest>(`${this.baseUrl}/requests`, request);
  }
}