import { Component, Input, OnInit, ChangeDetectorRef, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { SangrahApiService } from '../../../core/api/sangrah-api.service';

interface EventCollaborator {
  userId: string;
  email?: string;
  displayName?: string;
  canUploadMedia: boolean;
  canReviewMedia: boolean;
  canReviewAccessRequests: boolean;
  canDirectUpload: boolean;
  canDeleteMedia: boolean;
  canEditEventDetails: boolean;
  addedAt?: string;
  addedByUserId?: string;
  showPermissions?: boolean;
}

@Component({
  selector: 'app-event-collaborators',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="bg-white dark:bg-slate-900 rounded-2xl shadow-md border border-slate-200 dark:border-slate-800 text-slate-900 dark:text-slate-100 max-w-3xl overflow-hidden mt-6 mb-12">
      
      <!-- Header -->
      <div class="px-6 py-5 border-b border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-900/50">
        <h2 class="text-xl font-bold flex items-center gap-2.5">
          <svg xmlns="http://www.w3.org/2000/svg" class="h-6 w-6 text-slate-500" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z" />
          </svg>
          Manage Collaborators
        </h2>
        <p class="text-sm text-slate-500 dark:text-slate-400 mt-1 ml-8">Control who can access and contribute to this event</p>
      </div>

      <div class="p-6">
        <!-- Tabs -->
        <div class="flex bg-slate-100 p-1 flex-wrap rounded-lg mb-6 dark:bg-slate-800">
          <button 
            (click)="currentTab = 'collaborators'" 
            [class.bg-white]="currentTab === 'collaborators'"
            [class.dark:bg-slate-700]="currentTab === 'collaborators'"
            [class.shadow-sm]="currentTab === 'collaborators'"
            [class.text-slate-900]="currentTab === 'collaborators'"
            [class.dark:text-white]="currentTab === 'collaborators'"
            class="flex-1 py-2 px-4 rounded-md text-sm font-medium transition-all text-center">
            Collaborators ({{ collaborators.length }})
          </button>
          <button 
            (click)="currentTab = 'add'" 
            [class.bg-white]="currentTab === 'add'"
            [class.dark:bg-slate-700]="currentTab === 'add'"
            [class.shadow-sm]="currentTab === 'add'"
            [class.text-slate-900]="currentTab === 'add'"
            [class.dark:text-white]="currentTab === 'add'"
            class="flex-1 py-2 px-4 rounded-md text-sm font-medium transition-all text-center">
            Add People
          </button>
        </div>

        <!-- Tab: Collaborators -->
        <div *ngIf="currentTab === 'collaborators'" class="space-y-3">
          <div *ngIf="collaborators.length === 0" class="text-center py-8 text-slate-500 border border-dashed border-slate-300 dark:border-slate-700 rounded-xl bg-slate-50 dark:bg-slate-800/20">
            No collaborators added yet.
          </div>

          <div *ngFor="let collab of collaborators" 
               class="border border-slate-200 dark:border-slate-700 rounded-lg p-4 bg-white dark:bg-slate-800/40 hover:border-slate-300 dark:hover:border-slate-600 transition">
            
            <div class="flex items-center justify-between mb-3">
              <div class="flex items-center gap-3">
                <div class="h-10 w-10 rounded-full bg-slate-200 dark:bg-slate-700 flex items-center justify-center text-slate-600 dark:text-slate-300 font-bold text-sm">
                  {{ (collab.displayName || collab.email || 'U').charAt(0).toUpperCase() }}
                </div>
                <div>
                  <p class="font-semibold text-slate-900 dark:text-white text-sm">{{ collab.displayName || collab.email || collab.userId }}</p>
                  <p class="text-xs text-slate-500">{{ collab.email || 'Collaborator' }}</p>
                </div>
              </div>

              <div class="flex items-center gap-2">
                <button 
                  (click)="collab.showPermissions = !collab.showPermissions"
                  class="px-3 py-1.5 bg-white dark:bg-slate-800 border border-slate-300 dark:border-slate-600 text-slate-700 dark:text-slate-200 text-xs font-semibold rounded-lg hover:bg-slate-50 dark:hover:bg-slate-700 transition">
                  Edit Access
                </button>
                <button
                  (click)="removeCollaborator(collab.userId)"
                  class="px-2 py-1.5 text-slate-400 hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20 rounded transition">
                  🗑️
                </button>
              </div>
            </div>

            <!-- Expanded Permissions -->
            <div *ngIf="collab.showPermissions" class="mt-3 pt-3 border-t border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-800/30 -mx-4 -mb-4 p-4">
              <p class="text-xs font-bold text-slate-600 dark:text-slate-300 mb-3 uppercase">Permissions</p>
              <div class="grid grid-cols-1 md:grid-cols-2 gap-2 text-sm">
                <label class="flex items-center gap-2 text-slate-700 dark:text-slate-200 cursor-pointer">
                  <input [(ngModel)]="collab.canUploadMedia" (change)="updatePermissions(collab.userId, collab)" type="checkbox" class="w-4 h-4">
                  <span class="text-sm">Can Upload Media</span>
                </label>
                <label class="flex items-center gap-2 text-slate-700 dark:text-slate-200 cursor-pointer">
                  <input [(ngModel)]="collab.canDirectUpload" (change)="updatePermissions(collab.userId, collab)" type="checkbox" class="w-4 h-4">
                  <span class="text-sm">Can Direct Upload</span>
                </label>
                <label class="flex items-center gap-2 text-slate-700 dark:text-slate-200 cursor-pointer">
                  <input [(ngModel)]="collab.canReviewMedia" (change)="updatePermissions(collab.userId, collab)" type="checkbox" class="w-4 h-4">
                  <span class="text-sm">Can Approve Media</span>
                </label>
                <label class="flex items-center gap-2 text-slate-700 dark:text-slate-200 cursor-pointer">
                  <input [(ngModel)]="collab.canReviewAccessRequests" (change)="updatePermissions(collab.userId, collab)" type="checkbox" class="w-4 h-4">
                  <span class="text-sm">Review Access Requests</span>
                </label>
                <label class="flex items-center gap-2 text-slate-700 dark:text-slate-200 cursor-pointer">
                  <input [(ngModel)]="collab.canDeleteMedia" (change)="updatePermissions(collab.userId, collab)" type="checkbox" class="w-4 h-4">
                  <span class="text-sm">Can Delete Media</span>
                </label>
                <label class="flex items-center gap-2 text-slate-700 dark:text-slate-200 cursor-pointer">
                  <input [(ngModel)]="collab.canEditEventDetails" (change)="updatePermissions(collab.userId, collab)" type="checkbox" class="w-4 h-4">
                  <span class="text-sm">Can Edit Event</span>
                </label>
              </div>
            </div>
          </div>
        </div>

        <!-- Tab: Add People -->
        <div *ngIf="currentTab === 'add'" class="space-y-4">
          <p class="text-sm font-semibold text-slate-800 dark:text-slate-200">Search for users by email</p>
          <div class="relative">
            <input 
              [(ngModel)]="searchQuery" 
              (keyup.enter)="startAddingUser()"
              type="email" 
              placeholder="Enter email address to invite..." 
              class="w-full bg-white dark:bg-slate-800 border border-slate-300 dark:border-slate-600 rounded-lg py-2.5 pl-3 pr-4 text-slate-900 dark:text-white placeholder-slate-400 focus:border-blue-500 focus:ring-1 focus:ring-blue-500 outline-none">
          </div>

          <div *ngIf="searchQuery && searchQuery.includes('@')" class="mt-4">
            <div class="border border-slate-200 dark:border-slate-700 rounded-lg p-3 flex items-center justify-between bg-white dark:bg-slate-800">
              <div class="flex items-center gap-3">
                <div class="h-10 w-10 rounded-full bg-blue-100 dark:bg-blue-900/30 text-blue-600 dark:text-blue-400 flex items-center justify-center font-bold text-sm">
                  {{ searchQuery.charAt(0).toUpperCase() }}
                </div>
                <div>
                  <p class="font-semibold text-sm text-slate-900 dark:text-white">{{ searchQuery }}</p>
                </div>
              </div>
              <button 
                (click)="startAddingUser()"
                class="px-4 py-1.5 bg-black text-white dark:bg-white dark:text-black hover:bg-slate-800 dark:hover:bg-slate-200 text-sm font-semibold rounded-lg transition">
                Add
              </button>
            </div>
          </div>

          <!-- Permission Configuration -->
          <div *ngIf="stagedUserEmail" class="mt-6 pt-6 border-t border-slate-200 dark:border-slate-700">
            <h3 class="text-sm font-bold text-slate-900 dark:text-white mb-4">Configure access for {{ stagedUserEmail }}</h3>
            
            <div class="grid grid-cols-1 md:grid-cols-2 gap-3 mb-6">
              <label class="flex items-center justify-between p-3 border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 rounded-lg cursor-pointer hover:border-blue-400 transition">
                <span class="text-sm font-medium text-slate-700 dark:text-slate-200">Can Upload Media</span>
                <input [(ngModel)]="newCollaboratorPermissions.canUploadMedia" type="checkbox" class="w-4 h-4 text-blue-600">
              </label>
              <label class="flex items-center justify-between p-3 border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 rounded-lg cursor-pointer hover:border-blue-400 transition">
                <span class="text-sm font-medium text-slate-700 dark:text-slate-200">Can Direct Upload</span>
                <input [(ngModel)]="newCollaboratorPermissions.canDirectUpload" type="checkbox" class="w-4 h-4 text-blue-600">
              </label>
              <label class="flex items-center justify-between p-3 border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 rounded-lg cursor-pointer hover:border-blue-400 transition">
                <span class="text-sm font-medium text-slate-700 dark:text-slate-200">Can Approve Media</span>
                <input [(ngModel)]="newCollaboratorPermissions.canReviewMedia" type="checkbox" class="w-4 h-4 text-blue-600">
              </label>
              <label class="flex items-center justify-between p-3 border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 rounded-lg cursor-pointer hover:border-blue-400 transition">
                <span class="text-sm font-medium text-slate-700 dark:text-slate-200">Review Access Requests</span>
                <input [(ngModel)]="newCollaboratorPermissions.canReviewAccessRequests" type="checkbox" class="w-4 h-4 text-blue-600">
              </label>
              <label class="flex items-center justify-between p-3 border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 rounded-lg cursor-pointer hover:border-blue-400 transition">
                <span class="text-sm font-medium text-slate-700 dark:text-slate-200">Can Delete Media</span>
                <input [(ngModel)]="newCollaboratorPermissions.canDeleteMedia" type="checkbox" class="w-4 h-4 text-blue-600">
              </label>
              <label class="flex items-center justify-between p-3 border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 rounded-lg cursor-pointer hover:border-blue-400 transition">
                <span class="text-sm font-medium text-slate-700 dark:text-slate-200">Can Edit Event</span>
                <input [(ngModel)]="newCollaboratorPermissions.canEditEventDetails" type="checkbox" class="w-4 h-4 text-blue-600">
              </label>
            </div>

            <div class="flex gap-3">
              <button 
                (click)="addCollaborator()"
                [disabled]="isAdding"
                class="flex-1 bg-blue-600 hover:bg-blue-700 disabled:bg-blue-400 text-white font-semibold py-2.5 px-4 rounded-lg transition">
                {{ isAdding ? '⏳ Adding...' : '✅ Add Collaborator' }}
              </button>
              <button 
                (click)="resetForm()"
                class="px-4 py-2.5 bg-slate-200 hover:bg-slate-300 dark:bg-slate-700 dark:hover:bg-slate-600 text-slate-800 dark:text-slate-200 font-medium rounded-lg transition">
                Cancel
              </button>
            </div>
          </div>
        </div>

    </div>
  `,
  styles: []
})
export class EventCollaboratorsComponent implements OnInit {
  @Input() eventId!: string;

  private api = inject(SangrahApiService);
  private cdr = inject(ChangeDetectorRef);

  currentTab: 'collaborators' | 'add' = 'collaborators';
  collaborators: EventCollaborator[] = [];
  
  searchQuery = '';
  stagedUserEmail = '';
  isAdding = false;

  newCollaboratorEmail = '';
  newCollaboratorPermissions = {
    canUploadMedia: true,
    canReviewMedia: false,
    canReviewAccessRequests: false,
    canDirectUpload: false,
    canDeleteMedia: false,
    canEditEventDetails: false
  };

  ngOnInit() {
    this.loadCollaborators();
  }

  loadCollaborators() {
    this.api.getEventCollaborators(this.eventId).subscribe({
      next: (data: any) => {
        if (Array.isArray(data)) {
          this.collaborators = data.map(c => ({...c, showPermissions: false}));
        } else if (data && data.collaborators) {
          this.collaborators = data.collaborators.map((c: any) => ({...c, showPermissions: false}));
        } else {
          this.collaborators = [];
        }
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('❌ Failed to load collaborators:', err);
        this.collaborators = [];
      }
    });
  }

  startAddingUser() {
    if (!this.searchQuery || !this.searchQuery.includes('@')) return;
    this.stagedUserEmail = this.searchQuery;
    this.newCollaboratorEmail = this.searchQuery;
    
    // Default: Can Upload Media checked
    this.newCollaboratorPermissions = {
      canUploadMedia: true,
      canReviewMedia: false,
      canReviewAccessRequests: false,
      canDirectUpload: false,
      canDeleteMedia: false,
      canEditEventDetails: false
    };
  }

  resetForm() {
    this.searchQuery = '';
    this.stagedUserEmail = '';
    this.newCollaboratorEmail = '';
  }

  addCollaborator() {
    if (!this.newCollaboratorEmail) return;

    this.isAdding = true;
    const payload = {
      userEmail: this.newCollaboratorEmail,
      permissions: {
        canUploadMedia: this.newCollaboratorPermissions.canUploadMedia,
        canReviewMedia: this.newCollaboratorPermissions.canReviewMedia,
        canReviewAccessRequests: this.newCollaboratorPermissions.canReviewAccessRequests,
        canDirectUpload: this.newCollaboratorPermissions.canDirectUpload,
        canDeleteMedia: this.newCollaboratorPermissions.canDeleteMedia,
        canEditEventDetails: this.newCollaboratorPermissions.canEditEventDetails
      }
    };

    this.api.addEventCollaborator(this.eventId, payload).subscribe({
      next: (response) => {
        // Ensure UI moves to the collaborators tab first, then refresh list.
        this.currentTab = 'collaborators';
        this.isAdding = false;
        this.resetForm();
        this.loadCollaborators();
        this.cdr.detectChanges();

        // Extra safety: ensure change detection runs in next tick so any
        // asynchronous template updates settle and the Add form is hidden.
        setTimeout(() => {
          this.currentTab = 'collaborators';
          this.cdr.detectChanges();
        }, 0);
      },
      error: (err) => {
        console.error('❌ [Add Collaborator] Failed:', err);
        alert('Failed to add collaborator. Verify email matches an existing user.');
        this.isAdding = false;
        this.cdr.detectChanges();
      }
    });
  }

  removeCollaborator(userId: string) {
    if (!userId || userId === 'null' || userId === '') {
      alert('Cannot remove this collaborator entry - corrupted data detected.');
      this.loadCollaborators();
      return;
    }

    if (confirm('Remove this collaborator?')) {
      this.api.removeEventCollaborator(this.eventId, userId).subscribe({
        next: () => {
          this.loadCollaborators();
        },
        error: (err) => {
          console.error('❌ Failed to remove collaborator:', err);
        }
      });
    }
  }

  updatePermissions(userId: string, collaborator: EventCollaborator) {
    if (!userId || userId === 'null' || userId === '') {
      this.loadCollaborators();
      return;
    }

    const payload = {
      permissions: {
        canUploadMedia: collaborator.canUploadMedia,
        canReviewMedia: collaborator.canReviewMedia,
        canReviewAccessRequests: collaborator.canReviewAccessRequests,
        canDirectUpload: collaborator.canDirectUpload,
        canDeleteMedia: collaborator.canDeleteMedia,
        canEditEventDetails: collaborator.canEditEventDetails
      }
    };

    this.api.updateCollaboratorPermissions(this.eventId, userId, payload).subscribe({
      next: () => {
        this.cdr.detectChanges();
      },
      error: (err) => {
        console.error('❌ Failed to update permissions:', err);
        this.loadCollaborators();
      }
    });
  }
}
