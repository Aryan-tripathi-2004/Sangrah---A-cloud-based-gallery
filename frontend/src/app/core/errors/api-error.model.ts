import { HttpErrorResponse } from '@angular/common/http';

export interface ProblemDetails {
  type?: string;
  title?: string;
  status?: number;
  detail?: string;
  instance?: string;
  timestamp?: string;
  errors?: Record<string, string> | any[];
  [key: string]: any;
}

/**
 * Normalized application-level API error conforming to RFC-9457 Problem Details.
 * Eliminates repetitive manual HTTP response parsing in components while preserving
 * backwards compatibility for existing error property checks.
 *
 * Implements Presentation Layer Error Masking: Technical 500 internal server error
 * traces are sanitized with a user-safe fallback to prevent backend leakage.
 */
export class NormalizedApiError extends Error {
  public readonly status: number;
  public readonly title: string;
  public readonly detail: string;
  public readonly type?: string;
  public readonly instance?: string;
  public readonly validationErrors?: Record<string, string>;
  public readonly raw?: HttpErrorResponse;

  /**
   * Backwards-compatible getter for legacy code inspecting `err.error.error` or `err.error.message`
   */
  public get error(): { error: string; message: string; detail: string; [key: string]: any } {
    return {
      error: this.message,
      message: this.message,
      detail: this.detail,
      title: this.title,
      status: this.status,
    };
  }

  constructor(httpError: HttpErrorResponse | any) {
    let status = 500;
    let title = 'Application Error';
    let detail = 'An unexpected error occurred. Please try again.';
    let type: string | undefined;
    let instance: string | undefined;
    let validationErrors: Record<string, string> | undefined;

    if (httpError instanceof HttpErrorResponse) {
      status = httpError.status || 500;
      title = httpError.statusText || 'Error';
      const body = httpError.error;

      if (status >= 500) {
        // Presentation Layer Error Masking: Mask internal server stack traces and technical details
        detail = 'An unexpected system error occurred. Please try again later.';
        title = 'System Error';
      } else if (typeof body === 'string') {
        detail = body;
      } else if (body && typeof body === 'object') {
        detail = body.detail || body.message || body.error || detail;
        title = body.title || title;
        type = body.type;
        instance = body.instance;
        if (body.errors && typeof body.errors === 'object' && !Array.isArray(body.errors)) {
          validationErrors = body.errors;
        } else if (Array.isArray(body.violations)) {
          validationErrors = {};
          for (const v of body.violations) {
            if (typeof v === 'object' && v !== null && v.field && v.message) {
              validationErrors[v.field] = v.message;
            } else if (typeof v === 'string') {
              const colonIdx = v.indexOf(':');
              if (colonIdx > 0) {
                validationErrors[v.substring(0, colonIdx).trim()] = v.substring(colonIdx + 1).trim();
              } else {
                validationErrors[v.trim()] = v.trim();
              }
            }
          }
        }
      } else if (httpError.message) {
        detail = httpError.message;
      }
    } else if (httpError instanceof Error) {
      detail = httpError.message;
    } else if (typeof httpError === 'string') {
      detail = httpError;
    }

    super(detail);
    this.name = 'NormalizedApiError';
    this.status = status;
    this.title = title;
    this.detail = detail;
    this.type = type;
    this.instance = instance;
    this.validationErrors = validationErrors;
    if (httpError instanceof HttpErrorResponse) {
      this.raw = httpError;
    }

    // Set the prototype explicitly for ES5/ES6 inheritance
    Object.setPrototypeOf(this, NormalizedApiError.prototype);
  }
}

/**
 * Safely extracts a user-readable error message using instanceof type guards
 * rather than blind type assertions.
 */
export function extractErrorMessage(error: unknown, fallbackMessage = 'An unexpected error occurred'): string {
  if (error instanceof NormalizedApiError) {
    return error.detail || error.message || fallbackMessage;
  }
  if (error instanceof HttpErrorResponse) {
    if (typeof error.error === 'string' && error.error.trim()) {
      return error.error;
    }
    if (error.error && typeof error.error === 'object') {
      const body = error.error as { error?: string; message?: string; detail?: string };
      return body.detail || body.message || body.error || error.message || fallbackMessage;
    }
    return error.message || fallbackMessage;
  }
  if (error instanceof Error) {
    return error.message;
  }
  if (typeof error === 'string' && error.trim()) {
    return error;
  }
  return fallbackMessage;
}
