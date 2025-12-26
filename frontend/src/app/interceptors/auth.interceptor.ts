import { HttpInterceptorFn, HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { ToastService } from '../services/toast.service';
import { environment } from '../../environments/environment';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const toastService = inject(ToastService);

  // Skip adding token for registration endpoint
  if (req.url === environment.registerUserEndpoint) {
    return next(req);
  }

  // Only add token for requests to the API
  if (req.url.startsWith(environment.apiUrl)) {
    // Add token if user is authenticated
    if (authService.isAuthenticated()) {
      const token = authService.getToken();

      if (token) {
        // Clone the request and add authorization header
        req = req.clone({
          setHeaders: {
            Authorization: `Bearer ${token}`,
          },
        });
      }
    }
  }

  // Handle the request and catch errors
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        // Show toast notification for unauthorized access
        toastService.addToast({ message: 'You are not allowed to access this resource', type: 'error' });

        // Note: We don't automatically logout here, just show the error
        // The user will be logged out on next interaction if token is expired
      }

      return throwError(() => error);
    })
  );
};
