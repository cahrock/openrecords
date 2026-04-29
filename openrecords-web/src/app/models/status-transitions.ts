import { FoiaRequestStatus } from './foia-request.model';

/**
 * Allowed status transitions per source status.
 * Mirrors FoiaRequestStatus.java's ALLOWED_TRANSITIONS map on the backend.
 */
export const ALLOWED_TRANSITIONS: Record<FoiaRequestStatus, FoiaRequestStatus[]> = {
  DRAFT: ['SUBMITTED'],
  SUBMITTED: ['ACKNOWLEDGED', 'REJECTED'],
  ACKNOWLEDGED: ['ASSIGNED', 'REJECTED', 'ON_HOLD'],
  ASSIGNED: ['UNDER_REVIEW', 'ON_HOLD', 'REJECTED'],
  UNDER_REVIEW: ['RESPONSIVE_RECORDS_FOUND', 'NO_RECORDS', 'ON_HOLD', 'REJECTED'],
  ON_HOLD: ['ASSIGNED', 'UNDER_REVIEW', 'REJECTED', 'CLOSED'],
  RESPONSIVE_RECORDS_FOUND: ['DOCUMENTS_RELEASED', 'ON_HOLD'],
  DOCUMENTS_RELEASED: ['CLOSED'],
  NO_RECORDS: ['CLOSED'],
  REJECTED: [],
  CLOSED: [],
};

export function getAllowedTransitions(status: FoiaRequestStatus): FoiaRequestStatus[] {
  return ALLOWED_TRANSITIONS[status] || [];
}