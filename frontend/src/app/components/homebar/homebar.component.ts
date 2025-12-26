import { NavigationEnd, Router, RouterModule } from '@angular/router';
import { Component, signal } from '@angular/core';
import { filter } from 'rxjs/internal/operators/filter';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { NavbarButtonComponent } from '../navbar-button/navbar-button.component';
import { LoginComponent } from '../login.component/login.component';

@Component({
  selector: 'app-homebar',
  imports: [MatButtonModule, MatIconModule, RouterModule, MatMenuModule, NavbarButtonComponent, LoginComponent],
  templateUrl: './homebar.component.html',
  styleUrl: './homebar.component.scss',
  standalone: true
})
export class HomebarComponent {
  constructor(private readonly router: Router){
    router.events.pipe(filter(event => event instanceof NavigationEnd)).subscribe(() => {
      const currentLinkIndex = this.links.findIndex (
        link =>
          link.path === `/${router.parseUrl(router.url).root.children['primary']?.segments[0].path}`
      );
      this.activeMenuItem.set(currentLinkIndex === -1 ? 0 : currentLinkIndex);
      this.isMobileMenuOpen.set(false);
    })
  }

  isMobileMenuOpen = signal(false);
  activeMenuItem = signal(0);

  links = [
    {name: 'Dashboard', path: '/dashbaord'},
    {name: 'Groups', path: '/groups'}
  ];
}
