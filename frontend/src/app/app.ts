import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { HomebarComponent } from './components/homebar/homebar.component';
import { CdkScrollable } from "@angular/cdk/scrolling";
import { ToastComponent } from "./components/toast/toast.component";

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, HomebarComponent, CdkScrollable, ToastComponent],
  templateUrl: './app.html',
  styleUrl: './app.scss'
})
export class App {
  protected readonly title = signal('studyconnect');
}
