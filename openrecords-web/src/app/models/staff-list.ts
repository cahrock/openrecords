import { RequesterSummary } from './foia-request.model';

/**
 * Mock list of staff users for the assignment dropdown on the detail page.
 *
 * Phase 6: hardcoded based on the seeded test users (IDs 2 and 3).
 * Phase 8+: replace with a `GET /api/v1/users?role=STAFF` API call.
 */
export const ASSIGNABLE_STAFF: RequesterSummary[] = [
  { id: 2, email: 'intake.officer@example.com', fullName: 'Intake Officer' },
  { id: 3, email: 'case.officer@example.com',    fullName: 'Case Officer' },
];