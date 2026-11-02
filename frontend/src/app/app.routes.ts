import { Routes } from '@angular/router';
import { authGuard, publicGuard } from './core/auth/auth.guard';
import { eventAccessGuard } from './features/events/event-access.guard';

export const routes: Routes = [
  // Public routes
  {
    path: '',
    loadComponent: () => import('./features/home/home.component').then(m => m.HomeComponent),
    data: { title: 'Home' }
  },
  {
    path: 'about',
    loadComponent: () => import('./features/about/about.component').then(m => m.AboutComponent),
    data: { title: 'About' }
  },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login.component').then(m => m.LoginComponent),
    canActivate: [publicGuard],
    data: { title: 'Login' }
  },
  {
    path: 'signup',
    loadComponent: () => import('./features/auth/signup.component').then(m => m.SignupComponent),
    canActivate: [publicGuard],
    data: { title: 'Sign Up' }
  },

  // Protected routes
  {
    path: 'dashboard',
    loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent),
    canActivate: [authGuard],
    data: { title: 'Dashboard' }
  },
  {
    path: 'event',
    loadComponent: () => import('./features/events/events-list.component').then(m => m.EventsListComponent),
    canActivate: [authGuard],
    data: { title: 'Events' }
  },
  {
    path: 'event/create',
    loadComponent: () => import('./features/events/event-create.component').then(m => m.EventCreateComponent),
    canActivate: [authGuard],
    data: { title: 'Create Event' }
  },
  {
    path: 'event/:id',
    loadComponent: () => import('./features/events/event-detail.component').then(m => m.EventDetailComponent),
    canActivate: [authGuard, eventAccessGuard],
    data: { title: 'Event Details' }
  },
  {
    path: 'gallery',
    loadComponent: () => import('./features/gallery/gallery-page.component').then(m => m.GalleryPageComponent),
    canActivate: [authGuard],
    data: { title: 'Gallery' }
  },
  {
    path: 'billing',
    loadComponent: () => import('./features/billing/billing-page.component').then(m => m.BillingPageComponent),
    canActivate: [authGuard],
    data: { title: 'Billing' }
  },
  {
    path: 'pending-requests',
    loadComponent: () => import('./features/pending-requests/pending-requests.component').then(m => m.PendingRequestsComponent),
    canActivate: [authGuard],
    data: { title: 'Pending Requests' }
  },
  {
    path: 'pending-requests/:eventId',
    loadComponent: () => import('./features/pending-requests/pending-requests-detail.component').then(m => m.PendingRequestsDetailComponent),
    canActivate: [authGuard],
    data: { title: 'Manage Access Requests' }
  },
  {
    path: 'profile',
    loadComponent: () => import('./features/profile/profile.component').then(m => m.ProfileComponent),
    canActivate: [authGuard],
    data: { title: 'Profile' }
  },
  {
    path: 'settings',
    loadComponent: () => import('./features/settings/settings.component').then(m => m.SettingsComponent),
    canActivate: [authGuard],
    data: { title: 'Account Settings' }
  },

  // Backward compatibility routes
  { path: 'register', redirectTo: 'signup' },
  { path: 'events', redirectTo: 'event' },
  { path: 'events/:eventId', redirectTo: 'event/:eventId' },

  // Wildcard must be last
  { path: '**', redirectTo: '' },
];
