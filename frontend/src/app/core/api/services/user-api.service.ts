import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { User } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class UserApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8080/api/v1';

  getProfile(): Observable<User> {
    return this.http.get<User>(`${this.baseUrl}/users/profile`);
  }

  updateProfile(payload: Partial<User>): Observable<User> {
    return this.http.put<User>(`${this.baseUrl}/users/profile`, payload);
  }

  uploadProfileAvatar(file: File): Observable<User> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<User>(`${this.baseUrl}/users/profile/avatar`, formData);
  }

  deleteAccount(): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/users/profile`);
  }
}
