import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { NotificationListResponse } from '../models/api.models';

@Injectable({ providedIn: 'root' })
export class NotificationApiService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = 'http://localhost:8080/api/v1';

  getUnreadNotificationCount(): Observable<{ unreadCount: number }> {
    return this.http.get<{ unreadCount: number }>(`${this.baseUrl}/notifications/unread-count`);
  }

  getNotifications(skip: number = 0, limit: number = 50): Observable<NotificationListResponse> {
    const params = new HttpParams()
      .set('skip', skip.toString())
      .set('limit', limit.toString());
    return this.http.get<NotificationListResponse>(`${this.baseUrl}/notifications`, { params });
  }

  markNotificationRead(notificationId: string): Observable<{ success?: boolean; [key: string]: unknown }> {
    return this.http.patch<{ success?: boolean; [key: string]: unknown }>(`${this.baseUrl}/notifications/status`, {
      notificationIds: [notificationId],
      read: true
    });
  }

  markAllNotificationsRead(): Observable<{ success?: boolean; [key: string]: unknown }> {
    return this.http.patch<{ success?: boolean; [key: string]: unknown }>(`${this.baseUrl}/notifications/status`, {
      notificationIds: [],
      read: true
    });
  }
}
