import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { LayoutComponent } from '../../shared/layout/layout.component';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterLink, LayoutComponent],
  templateUrl: './home.component.html',
})
export class HomeComponent {}
