import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import Keycloak from 'keycloak-js';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly keycloak: Keycloak;
  private readonly isAuthenticatedSubject = new BehaviorSubject<boolean>(false);
  public readonly isAuthenticated$ = this.isAuthenticatedSubject.asObservable();
  private tokenExpirationTimer: any;

  constructor() {
    this.keycloak = new Keycloak({
      url: environment.identityProviderUrl,
      realm: environment.realmName,
      clientId: environment.clientID,
    });
  }

  /**
   * Initialize Keycloak instance
   */
  async init(): Promise<boolean> {
    try {
      const authenticated = await this.keycloak.init({
        onLoad: 'check-sso',
        silentCheckSsoRedirectUri: globalThis.location.origin + '/assets/silent-check-sso.html',
        pkceMethod: 'S256',
        flow: 'standard',
        checkLoginIframe: false,
      });

      this.isAuthenticatedSubject.next(authenticated);

      if (authenticated) {
        this.setupTokenRefresh();
      }

      if (environment.debugAuth) {
        this.logRoles('init');
      }

      this.keycloak.onAuthSuccess = () => {
        this.isAuthenticatedSubject.next(true);
        if (environment.debugAuth) {
          this.logRoles('onAuthSuccess');
        }
      };
      this.keycloak.onAuthRefreshSuccess = () => {
        if (environment.debugAuth) {
          this.logRoles('onAuthRefreshSuccess');
        }
      };

      // Handle token expiration
      this.keycloak.onTokenExpired = () => {
        console.warn('Token expired, user will be logged out on next interaction');
        // Don't refresh automatically, logout on next interaction
      };

      return true;
    } catch (error) {
      console.error('Failed to initialize Keycloak', error);
      return false;
    }
  }

  /**
   * Setup automatic token refresh
   */
  private setupTokenRefresh(): void {
    if (this.tokenExpirationTimer) {
      clearInterval(this.tokenExpirationTimer);
    }

    // Check token validity every 10 seconds
    this.tokenExpirationTimer = setInterval(async () => {
      try {
        // If token is expired, logout
        if (this.keycloak.isTokenExpired()) {
          console.warn('Token expired, logging out user');
          await this.logout();
        } else {
          // Try to refresh token if it expires in less than 70 seconds
          await this.keycloak.updateToken(70);
        }
      } catch (error) {
        console.error('Failed to refresh token', error);
        await this.logout();
      }
    }, 10000);
  }

  /**
   * Login user by redirecting to Keycloak
   */
  login(redirectUri?: string): Promise<void> {
    const options: Keycloak.KeycloakLoginOptions = {
      redirectUri: redirectUri || globalThis.location.origin + '/dashboard', // Placeholder route
    };
    return this.keycloak.login(options);
  }

  /**
   * Logout user
   */
  async logout(): Promise<void> {
    if (this.tokenExpirationTimer) {
      clearInterval(this.tokenExpirationTimer);
    }
    this.isAuthenticatedSubject.next(false);
    await this.keycloak.logout({
      redirectUri: globalThis.location.origin + '/login',
    });
  }

  /**
   * Get current access token
   */
  getToken(): string | undefined {
    // Check if token is expired before returning
    if (this.keycloak.isTokenExpired()) {
      console.warn('Token is expired');
      return undefined;
    }
    return this.keycloak.token;
  }

  /**
   * Check if user is authenticated
   */
  isAuthenticated(): boolean {
    if (!environment.production) {
      // In development mode, always return true (optional authentication)
      return this.keycloak.authenticated || false;
    }
    // Check if token is still valid
    return this.keycloak.authenticated === true && !this.keycloak.isTokenExpired();
  }

  /**
   * Check if user has required role
   */
  hasRole(role: string): boolean {
    if (!environment.production) {
      // In development mode, bypass role checking if not authenticated
      if (!this.isAuthenticated()) {
        return true;
      }
    }

    // Support both realm-roles and client-roles (resource roles).
    try {
      return (
        this.keycloak.hasRealmRole(role) ||
        this.keycloak.hasResourceRole(role) ||
        this.keycloak.hasResourceRole(role, environment.clientID)
      );
    } catch {
      const realmRoles = this.keycloak.realmAccess?.roles ?? [];
      const clientRoles = this.keycloak.resourceAccess?.[environment.clientID]?.roles ?? [];
      return realmRoles.includes(role) || clientRoles.includes(role);
    }
  }

  /**
   * Check if user has the required role from environment
   */
  hasRequiredRole(): boolean {
    return this.hasRole(environment.requiredRole);
  }

  /**
   * Get user profile information from Keycloak
   */
  async getUserProfile(): Promise<Keycloak.KeycloakProfile | undefined> {
    try {
      return await this.keycloak.loadUserProfile();
    } catch (error) {
      console.error('Failed to load user profile', error);
      return undefined;
    }
  }

  /**
   * Get user's email from Keycloak profile
   */
  async getUserEmail(): Promise<string | undefined> {
    const claims = this.getTokenClaims();
    return this.getClaimString(claims, 'email');
  }

  async getFullName(): Promise<string | undefined> {
    const claims = this.getTokenClaims();
    const fullName = this.getClaimString(claims, 'name');
    if (fullName) {
      return fullName;
    }

    const firstName = this.getClaimString(claims, 'given_name') ?? this.getClaimString(claims, 'firstName');
    const lastName = this.getClaimString(claims, 'family_name') ?? this.getClaimString(claims, 'lastName');
    if (firstName && lastName) {
      return `${firstName} ${lastName}`;
    }

    return undefined;
  }

  private getTokenClaims(): Record<string, unknown> | undefined {
    return (this.keycloak.idTokenParsed ?? this.keycloak.tokenParsed) as Record<string, unknown> | undefined;
  }

  private getClaimString(claims: Record<string, unknown> | undefined, key: string): string | undefined {
    const value = claims?.[key];
    return typeof value === 'string' ? value : undefined;
  }

  private logRoles(context: string): void {
    const realmRoles = this.keycloak.realmAccess?.roles ?? [];
    const resourceAccess = this.keycloak.resourceAccess ?? {};
    const resourceRoles: Record<string, string[]> = {};

    for (const [client, access] of Object.entries(resourceAccess)) {
      const roles = (access as { roles?: string[] } | undefined)?.roles;
      resourceRoles[client] = Array.isArray(roles) ? roles : [];
    }

    console.info('[AuthService]', context, {
      authenticated: this.keycloak.authenticated ?? false,
      realm: environment.realmName,
      clientId: environment.clientID,
      requiredRole: environment.requiredRole,
      realmRoles,
      resourceRoles,
    });
  }
}
