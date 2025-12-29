import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';
import { RegisterComponent } from './pages/register/register.component';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { GroupsComponent } from './pages/groups/groups.component';
import { UnauthorizedComponent } from './pages/unauthorized.component/unauthorized.component';
import { NotAllowedComponent } from './pages/not-allowed/not-allowed.component';

export const routes: Routes = [
  {
    path: 'register',
    component: RegisterComponent,
  },
  {
    path: 'dashboard',
    component: DashboardComponent,
    canActivate: [authGuard],
  },
  {
    path: 'groups',
    component: GroupsComponent,
    canActivate: [authGuard],
  },
  {
    path: 'unauthorized',
    component: UnauthorizedComponent
  },
  {
    path: 'not-allowed',
    component: NotAllowedComponent,
  },
  {
    path: '',
    redirectTo: '/dashboard',
    pathMatch: 'full',
  },
  {
    path: '**',
    redirectTo: '/dashboard',
  },
];
