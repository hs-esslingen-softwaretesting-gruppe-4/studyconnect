import { NgClass } from '@angular/common';
import { Component, Input } from '@angular/core';
import { RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'app-navbar-button',
  imports: [RouterLinkActive, RouterLink, NgClass],
  templateUrl: './navbar-button.component.html',
  styleUrl: './navbar-button.component.scss',
})
export class NavbarButtonComponent {
  @Input() label!: string;
  @Input() routerLink!: string;
  @Input() isExact: boolean = false;
  @Input() extraClasses: string = "";

}
