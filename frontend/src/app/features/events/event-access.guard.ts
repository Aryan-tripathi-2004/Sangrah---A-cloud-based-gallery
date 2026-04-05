import { inject, Injectable } from '@angular/core';
import { CanActivateFn, Router, ActivatedRouteSnapshot, UrlTree } from '@angular/router';
import { SangrahApiService } from '../../core/api/sangrah-api.service';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

/**
 * Guard to prevent navigating to event detail when user doesn't have access.
 * If the event is PROTECTED and the backend indicates the user requires approval,
 * redirect to the events list and open the access request modal via query param.
 */
export const eventAccessGuard: CanActivateFn = (route: ActivatedRouteSnapshot): Observable<boolean | UrlTree> => {
  const api = inject(SangrahApiService);
  const router = inject(Router);
  const eventId = route.paramMap.get('id') || '';

  if (!eventId) {
    return of(router.createUrlTree(['/event']));
  }

  return api.getEventById(eventId).pipe(
    map((event: any) => {
      // If protected and requiresApproval, redirect to /event with query param to open modal
      if (event && event.visibility === 'PROTECTED' && event.requiresApproval === true) {
        return router.createUrlTree(['/event'], { queryParams: { requestEventId: eventId } });
      }

      // If PRIVATE and backend still prevents viewing, redirect to list
      if (event && event.visibility === 'PRIVATE') {
        // Backend will return 403 for private when user not allowed; if we get here and user is owner/collab allow
        // Otherwise allow navigation
        return true;
      }

      // Allow navigation for public or approved users
      return true;
    }),
    catchError((err) => {
      // On errors (403 etc.) redirect to events list
      try {
        if (err && err.status === 403) {
          return of(router.createUrlTree(['/event'], { queryParams: { requestEventId: eventId } }));
        }
      } catch (e) { }
      return of(router.createUrlTree(['/event']));
    })
  );
};
