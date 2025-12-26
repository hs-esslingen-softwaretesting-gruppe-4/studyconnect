import { inject } from '@angular/core';
import { CanActivateFn, Router, ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { environment } from '../../environments/environment';

export const authGuard: CanActivateFn = (
  route: ActivatedRouteSnapshot,
  state: RouterStateSnapshot
) => {
  const authService = inject(AuthService);
  const router = inject(Router);

  // In development mode, always allow access
  if (!environment.production) {
    return true;
  }

  // In production mode, check authentication and role
  const isAuthenticated = authService.isAuthenticated();
  const hasRequiredRole = authService.hasRequiredRole();

  if (isAuthenticated && hasRequiredRole) {
    return true;
  }

  // Redirect to unauthorized page if not authenticated
  if(!isAuthenticated) {
    router.navigate(['/unauthorized']);
    return false;
  }

  // Redirect to not allowed page if role is insufficient
  if (!hasRequiredRole) {
    router.navigate(['/not-allowed']);
    return false;
  }
  return false;
};
