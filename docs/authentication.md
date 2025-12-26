# Keycloak Authentication Implementation

This document describes the Keycloak authentication and authorization implementation for the StudyConnect Angular frontend.

## Overview

The application uses Keycloak for authentication with OAuth2 Authorization Code + PKCE flow. Authentication is conditionally enforced based on the `production` flag in the environment configuration.

## Features

- **Keycloak Integration**: Direct integration using `keycloak-js` library with Authorization Code + PKCE flow
- **Conditional Authorization**: Authentication is enforced only when `production: true`
- **JWT Token Management**: Automatic token attachment to API requests via HTTP interceptor
- **Automatic Token Refresh**: Tokens are automatically refreshed before expiration
- **Token Expiration Handling**: User is logged out on next interaction when token expires
- **Registration Flow**: User registration via backend endpoint with email pre-fill on login page
- **Role-Based Access Control**: Route protection based on Keycloak realm roles
- **Material Design UI**: Login and registration pages using Angular Material components
- **Error Handling**: Error banners for registration failures and toast notifications for 401 errors

## Architecture

### Services

#### AuthService (`src/app/services/auth.service.ts`)
Wraps Keycloak functionality and provides:
- `init()`: Initialize Keycloak instance with PKCE
- `login(redirectUri?)`: Redirect to Keycloak login page
- `logout()`: Logout user and redirect to login page
- `getToken()`: Retrieve current JWT access token
- `isAuthenticated()`: Check if user is authenticated
- `hasRole(role)`: Check if user has specific realm role
- `hasRequiredRole()`: Check if user has the role from environment config
- `register(userData)`: Register new user via backend endpoint

#### ToastService (`src/app/services/toast.service.ts`)
Provides toast notifications using Angular Material Snackbar:
- `success(message)`: Show success message
- `error(message)`: Show error message
- `info(message)`: Show info message
- `warning(message)`: Show warning message

### Interceptors

#### Auth Interceptor (`src/app/interceptors/auth.interceptor.ts`)
HTTP interceptor that:
- Adds `Authorization: Bearer <token>` header to API requests
- Skips token for registration endpoint
- Handles 401 errors with toast notifications
- Respects production flag for token enforcement

### Guards

#### Auth Guard (`src/app/guards/auth.guard.ts`)
Route guard that:
- Always allows access when `production: false`
- Checks authentication and required role when `production: true`
- Redirects to login with return URL on unauthorized access

### Components

#### LoginComponent (`src/app/pages/login/login.component.ts`)
- Email input field (pre-fillable from query parameter)
- Triggers Keycloak login flow on submit
- Supports return URL from query parameters

#### RegisterComponent (`src/app/pages/register/register.component.ts`)
- Reactive form with validation
- Calls backend registration endpoint
- Shows error banner on failure
- Redirects to login with email pre-filled on success

#### ErrorBannerComponent (`src/app/components/error-banner/error-banner.component.ts`)
- Displays error messages with Material Design
- Dismissible with close button

#### DashboardComponent (`src/app/pages/dashboard/dashboard.component.ts`)
- Placeholder protected route
- Demonstrates authenticated access
- Logout functionality

## Configuration

### Environment Variables

Both `environment.ts` (production) and `environment.development.ts` (development) should contain:

```typescript
export const environment = {
  production: true, // or false for development
  apiUrl: 'http://localhost:8080/api',
  registerUserEndpoint: 'http://localhost:8080/api/users',

  // Keycloak configuration
  identityProviderUrl: 'https://keycloak.example.com/auth',
  requiredRole: 'studyconnect',
  realmName: 'studyconnect',
  clientID: 'studyconnect-frontend',
};
```

### Keycloak Configuration

The Keycloak client should be configured with:
- **Client Type**: Public
- **Valid Redirect URIs**: `http://localhost:4200/*` (and production URLs)
- **Web Origins**: `http://localhost:4200` (and production URLs)
- **Standard Flow**: Enabled
- **Direct Access Grants**: Disabled (we use Authorization Code flow)

## Routing

The application has the following routes:

- `/login`: Public login page
- `/register`: Public registration page
- `/dashboard`: Protected route (placeholder)
- `/`: Redirects to `/dashboard`
- `**`: Catch-all redirects to `/dashboard`

All routes except `/login` and `/register` are protected by `authGuard`.

## Development Mode Behavior

When `production: false`:
- Keycloak is initialized but not enforced
- Users can access all routes without authentication
- Login is available but optional
- Token is still added to requests if user is authenticated

## Production Mode Behavior

When `production: true`:
- Keycloak authentication is enforced
- Unauthenticated users are redirected to `/login`
- Users must have the `requiredRole` to access protected routes
- Token expiration results in logout on next interaction

## Token Management

### Token Refresh
- Tokens are automatically refreshed every 10 seconds if they expire in less than 70 seconds
- Refresh happens silently in the background

### Token Expiration
- When a token expires, it is detected on the next HTTP request or navigation
- User is logged out and redirected to login page
- No automatic refresh on expiration (user must re-authenticate)

## Error Handling

### 401 Unauthorized
- Toast notification: "You are not allowed to access this resource"
- No automatic logout (user stays on current page)

### Registration Errors
- Error banner displayed at top of registration form
- Error message from backend or generic message

## User Flow

### Registration Flow
1. User navigates to `/register`
2. User fills out registration form
3. Form data sent to backend `registerUserEndpoint`
4. On success: Redirect to `/login?email=<registered-email>`
5. On failure: Display error banner

### Login Flow
1. User navigates to `/login`
2. Email field may be pre-filled from query parameter
3. User clicks "Login with Keycloak"
4. Redirected to Keycloak login page
5. After successful authentication, redirected back to `returnUrl` or `/dashboard`

### Authenticated Session
1. User accesses protected routes
2. JWT token automatically attached to API requests
3. Token automatically refreshed before expiration
4. On token expiration, user logged out on next interaction

## Dependencies

```json
{
  "keycloak-js": "^26.0.0",
  "@angular/material": "^21.0.0",
  "@angular/cdk": "^21.0.0",
  "@angular/animations": "^21.0.0"
}
```

## Testing

To test the implementation:

1. **Development Mode** (`production: false`):
   ```bash
   ng serve
   ```
   - Access should work without authentication
   - Login button still functional for testing

2. **Production Mode** (`production: true`):
   - Update `environment.development.ts` to set `production: true`
   - Ensure Keycloak server is running and accessible
   - Protected routes should require authentication

## Security Considerations

- PKCE (S256) is enabled for enhanced security
- Tokens are never stored in localStorage (managed by Keycloak)
- Silent SSO check iframe is disabled for better compatibility
- CORS must be configured on backend to allow frontend origin
- All backend API requests include JWT token in Authorization header

## Future Enhancements

- Implement remaining application pages (tasks, groups, etc.)
- Add comprehensive unit and integration tests
- Implement role-based UI element hiding
- Add loading indicators during Keycloak redirects
- Implement token refresh error handling with retry logic
- Add user profile page with Keycloak profile data
