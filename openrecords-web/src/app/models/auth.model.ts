/**
 * TypeScript types mirroring backend AuthService DTOs.
 *
 * Backend file: openrecords-api/src/main/java/com/openrecords/api/dto/
 *  - LoginRequest, RegisterRequest, VerifyEmailRequest
 *  - AuthResponse, RegistrationResponse, VerifyEmailResponse
 */

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  fullName: string;
}

export interface VerifyEmailRequest {
  token: string;
}

export interface AuthUser {
  id: number;
  email: string;
  fullName: string;
}

export type UserRole = 'REQUESTER' | 'STAFF' | 'ADMIN';

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  user: AuthUser;
  role: UserRole;
}

export interface RegistrationResponse {
  id: number;
  email: string;
  fullName: string;
  message: string;
}

export interface VerifyEmailResponse {
  email: string;
  message: string;
}

/**
 * Combined auth state stored in AuthService.
 */
export interface AuthSession {
  accessToken: string;
  refreshToken: string;
  user: AuthUser;
  role: UserRole;
}