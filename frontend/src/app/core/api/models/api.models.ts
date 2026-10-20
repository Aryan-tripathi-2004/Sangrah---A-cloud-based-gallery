export interface User {
  id: string;
  email: string;
  displayName: string;
  createdAt: string;
  bio?: string;
  avatarUrl?: string;
}

export interface LoginPayload {
  email: string;
  password: string;
}

export interface RegisterPayload {
  email: string;
  password: string;
  displayName: string;
}

export interface AuthResponse {
  token: string;
  refreshToken?: string;
  user: User;
}

export interface CollaboratorPermissions {
  canUploadMedia?: boolean;
  canReviewMedia?: boolean;
  canReviewAccessRequests?: boolean;
  canDirectUpload?: boolean;
  canDeleteMedia?: boolean;
  canEditEventDetails?: boolean;
}

export interface AddCollaboratorPayload {
  userEmail: string;
  permissions?: CollaboratorPermissions;
  canUploadMedia?: boolean;
  canReviewMedia?: boolean;
  canReviewAccessRequests?: boolean;
  canDirectUpload?: boolean;
  canDeleteMedia?: boolean;
  canEditEventDetails?: boolean;
}

export interface UpdateCollaboratorPermissionsPayload {
  permissions: CollaboratorPermissions;
}

export interface EventCollaborator {
  userId: string;
  canUploadMedia?: boolean;
  canReviewMedia?: boolean;
  canReviewAccessRequests?: boolean;
  canDirectUpload?: boolean;
  canDeleteMedia?: boolean;
  canEditEventDetails?: boolean;
  addedAt?: string;
  addedByUserId?: string;
  email?: string;
  displayName?: string;
}

export interface Event {
  id: string;
  eventId?: string;
  ownerUserId: string;
  title: string;
  description: string;
  eventDate: string;
  visibility: string;
  moderationEnabled: boolean;
  status: string;
  coverImageId?: string;
  createdAt: string;
  uploadPolicy?: string;
  moderationRequired?: boolean;
  requiresApproval?: boolean;
  accessStatus?: string;
  message?: string;
  collaborators?: EventCollaborator[];
}

export interface UpdateEventPayload {
  title: string;
  description?: string;
  eventDate: string;
  visibility: 'PUBLIC' | 'PROTECTED' | 'PRIVATE' | string;
  moderationEnabled: boolean;
}

export interface EventMediaUploadResponse {
  id: string;
  mediaId?: string;
  eventId: string;
  status: string;
  originalFileName?: string;
  mimeType?: string;
  sizeBytes?: number;
  uploadedAt?: string;
  message?: string;
}

export interface EventAccessRequest {
  id: string;
  eventId: string;
  userId: string;
  userDisplayName?: string;
  userEmail?: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'REVOKED';
  message?: string;
  requestedAt: string;
  reviewedAt?: string;
  approvalDuration?: string;
  accessExpiresAt?: string;
  reason?: string;
}

export interface AccessRequestResponse {
  id?: string;
  requestId?: string;
  eventId: string;
  userId?: string;
  status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'REVOKED';
  message?: string;
  approvalDuration?: string;
  accessExpiresAt?: string;
  reason?: string;
}

export interface AccessStatusResponse {
  eventId: string;
  userId: string;
  accessStatus: 'NO_ACCESS' | 'APPROVED' | 'PENDING' | 'REJECTED' | 'REVOKED';
  requiresApproval: boolean;
  canUpload: boolean;
  canView: boolean;
  message?: string;
}

export interface EventPolicies {
  eventId: string;
  moderationRequired: boolean;
  allowedDomains?: string[];
  maxFileSize?: number;
  [key: string]: unknown;
}

export interface Gallery {
  id: string;
  ownerUserId: string;
  originalFileName: string;
  mimeType: string;
  sizeBytes: number;
  uploadedAt: string;
  visibility: string;
}

export interface MediaItem {
  id: string;
  originalFileName: string;
  mimeType: string;
  sizeBytes: number;
  type: string;
  checksumSha256: string;
  metadata: Record<string, unknown>;
  uploadedAt: string;
  deletedAt?: string;
}

export interface StorageUsage {
  totalBytesUsed: number;
  fileCount: number;
  breakdown: {
    imageBytes: number;
    videoBytes: number;
  };
  formatted: {
    totalFormatted: string;
    imageFormatted: string;
    videoFormatted: string;
  };
  message: string;
}

export interface TimelineGroup {
  type: string;
  groupType: string;
  label: string;
  count: number;
  items: MediaItem[];
}

export interface TimelineResponse {
  view: string;
  groups: TimelineGroup[];
  totalGroups: number;
}

export interface Notification {
  id: string;
  recipientUserId: string;
  type: string;
  payload: Record<string, unknown>;
  read: boolean;
  createdAt: string;
}

export interface NotificationListResponse {
  notifications: Notification[];
  unreadCount: number;
  total?: number;
}

export interface CostEstimateDTO {
  currentMonthData: {
    startDate: string;
    daysElapsed: number;
    estimatedGBDays: number;
    estimatedCost: number;
    projectedMonthlyTotal: number;
  };
  storageBreakdown: {
    images: { gbDays: number; cost: number };
    videos: { gbDays: number; cost: number };
  };
  costPerDay: number;
  ratePerGBDay: number;
}

export interface InvoiceDTO {
  id: string;
  invoiceId: string;
  userId: string;
  billingPeriod: {
    startDate: string;
    endDate: string;
  };
  storageMetrics: {
    imageGBDays: number;
    imageCost: number;
    videoGBDays: number;
    videoCost: number;
    totalGBDays: number;
  };
  charges: {
    storageRate: number;
    subtotal: number;
    taxRate: number;
    tax: number;
    totalAmount: number;
  };
  status: 'PENDING' | 'PAID' | 'OVERDUE';
  issuedDate: string;
  dueDate: string;
  paidDate: string | null;
}

export interface PaymentDTO {
  id: string;
  invoiceId: string;
  amount: number;
  status: 'SUCCESS' | 'FAILED' | 'PENDING';
  transactionDate: string;
  stripeChargeId?: string;
  failureReason?: string;
}

export interface CheckoutSessionResponse {
  invoiceId: string;
  clientSecret: string;
  amount: number;
  currency: string;
}

export interface PaymentStatusResponse {
  status: string;
  paidDate?: string;
}

export interface PagedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
  isEmpty: boolean;
}
