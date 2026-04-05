import { Routes } from '@angular/router';
import { authGuard, publicGuard } from './core/auth/auth.guard';
import { HomeComponent } from './features/home/home.component';
import { LoginComponent } from './features/auth/login.component';
import { SignupComponent } from './features/auth/signup.component';
import { DashboardComponent } from './features/dashboard/dashboard.component';
import { EventsListComponent } from './features/events/events-list.component';
import { EventDetailComponent } from './features/events/event-detail.component';
import { EventCreateComponent } from './features/events/event-create.component';
import { eventAccessGuard } from './features/events/event-access.guard';
import { GalleryPageComponent } from './features/gallery/gallery-page.component';
import { BillingPageComponent } from './features/billing/billing-page.component';
import { PendingRequestsComponent } from './features/pending-requests/pending-requests.component';
import { PendingRequestsDetailComponent } from './features/pending-requests/pending-requests-detail.component';
import { ProfileComponent } from './features/profile/profile.component';
import { AboutComponent } from './features/about/about.component';

export const routes: Routes = [
  // Public routes
  { path: '', component: HomeComponent, data: { title: 'Home' } },
  { path: 'about', component: AboutComponent, data: { title: 'About' } },
  { path: 'login', component: LoginComponent, canActivate: [publicGuard], data: { title: 'Login' } },
  { path: 'signup', component: SignupComponent, canActivate: [publicGuard], data: { title: 'Sign Up' } },

  // Protected routes
  { path: 'dashboard', component: DashboardComponent, canActivate: [authGuard], data: { title: 'Dashboard' } },
  { path: 'event', component: EventsListComponent, canActivate: [authGuard], data: { title: 'Events' } },
  { path: 'event/create', component: EventCreateComponent, canActivate: [authGuard], data: { title: 'Create Event' } },
  { path: 'event/:id', component: EventDetailComponent, canActivate: [authGuard, eventAccessGuard], data: { title: 'Event Details' } },
  { path: 'gallery', component: GalleryPageComponent, canActivate: [authGuard], data: { title: 'Gallery' } },
  { path: 'billing', component: BillingPageComponent, canActivate: [authGuard], data: { title: 'Billing' } },
  { path: 'pending-requests', component: PendingRequestsComponent, canActivate: [authGuard], data: { title: 'Pending Requests' } },
  { path: 'pending-requests/:eventId', component: PendingRequestsDetailComponent, canActivate: [authGuard], data: { title: 'Manage Access Requests' } },
  { path: 'profile', component: ProfileComponent, canActivate: [authGuard], data: { title: 'Profile' } },

  // Backward compatibility routes
  { path: 'register', redirectTo: 'signup' },
  { path: 'events', redirectTo: 'event' },
  { path: 'events/:eventId', redirectTo: 'event/:eventId' },

  // Wildcard must be last
  { path: '**', redirectTo: '' },
];

