/**
 * TypeScript types mirroring the backend DTOs.
 *
 * Keep these in sync with:
 *   - CreateFoiaRequestDto.java
 *   - FoiaRequestDto.java
 *   - RequesterSummaryDto.java
 *   - PageDto.java
 */

/**
 * Status state machine for FOIA requests.
 * Mirrors FoiaRequestStatus.java on the backend.
 */
export type FoiaRequestStatus =
  | 'DRAFT'
  | 'SUBMITTED'
  | 'ACKNOWLEDGED'
  | 'ASSIGNED'
  | 'UNDER_REVIEW'
  | 'ON_HOLD'
  | 'RESPONSIVE_RECORDS_FOUND'
  | 'NO_RECORDS'
  | 'DOCUMENTS_RELEASED'
  | 'REJECTED'
  | 'CLOSED';

/**
 * Terminal statuses — used by the UI to style "closed" requests differently.
 */
export const TERMINAL_STATUSES: ReadonlySet<FoiaRequestStatus> = new Set([
  'CLOSED',
  'REJECTED',
  'DOCUMENTS_RELEASED',
  'NO_RECORDS',
]);

export interface RequesterSummary {
  id: number;
  email: string;
  fullName: string;
}

/**
 * Outgoing: what we POST to create a new request.
 * Matches CreateFoiaRequestDto.java.
 */
export interface CreateFoiaRequest {
  subject: string;
  description: string;
  recordsRequested: string;
  dateRangeStart?: string;
  dateRangeEnd?: string;
  feeWaiverRequested?: boolean;
  maxFeeWilling?: number;
}

/**
 * Incoming: full request as returned from the API.
 * Matches FoiaRequestDto.java.
 */
export interface FoiaRequest {
  id: string;
  trackingNumber: string;
  requester: RequesterSummary;
  subject: string;
  description: string;
  recordsRequested: string;
  dateRangeStart: string | null;
  dateRangeEnd: string | null;
  status: FoiaRequestStatus;
  feeWaiverRequested: boolean;
  maxFeeWilling: number | null;
  submittedAt: string | null;
  acknowledgedAt: string | null;
  dueDate: string | null;
  closedAt: string | null;
  createdAt: string;
}

/**
 * Paginated response wrapper.
 * Matches PageDto.java.
 */
export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}