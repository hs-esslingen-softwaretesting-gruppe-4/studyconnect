import { Routes } from '@angular/router';
import { authGuard } from './guards/auth.guard';
import { RegisterComponent } from './pages/register/register.component';
import { DashboardComponent } from './pages/dashboard/dashboard.component';
import { GroupsComponent } from './pages/groups/groups.component';
import { UnauthorizedComponent } from './pages/unauthorized.component/unauthorized.component';
import { NotAllowedComponent } from './pages/not-allowed/not-allowed.component';
import { UserResolver } from './resolver/user.resolver';
import { GroupResolver } from './resolver/group.resolver';
import { GroupsDetailComponent } from './pages/groups-detail/groups-detail.component';
import { NotFoundComponent } from './pages/not-found/not-found.component';
import { AllUserResolver } from './resolver/all-user.resolver';
import { CreateGroupComponent } from './pages/create-group/create-group.component';

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
    path: 'groups/create',
    component: CreateGroupComponent,
    canActivate: [authGuard],
    resolve: {userId: UserResolver, users: AllUserResolver},
  },
  {
    path: 'groups/:groupId',
    component: GroupsDetailComponent,
    canActivate: [authGuard],
    resolve: {userId: UserResolver, groupResolvedData: GroupResolver, users: AllUserResolver},
  },
  {
    path: 'groups',
    component: GroupsComponent,
    canActivate: [authGuard],
    resolve: {userId: UserResolver},
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
    path: 'not-found',
    component: NotFoundComponent,
  },
  {
    path: '',
    component: NotFoundComponent,
  },
  {
    path: '**',
    component: NotFoundComponent,
  },
];
