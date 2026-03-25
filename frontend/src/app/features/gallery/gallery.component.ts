import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SangrahApiService, MediaItem } from '../../core/api/sangrah-api.service';
import { LayoutComponent } from '../../shared/layout/layout.component';

@Component({
  selector: 'app-gallery',
  standalone: true,
  imports: [CommonModule, LayoutComponent],
  template: `
    <app-layout>
      <div class="space-y-8">
        <!-- Header -->
        <div class="flex items-center justify-between">
          <div>
            <h1 class="text-4xl font-bold mb-2">My Gallery</h1>
            <p class="text-slate-400">Your personal collection of photos and memories</p>
          </div>
          <button class="px-6 py-3 bg-gradient-to-r from-blue-500 to-purple-600 hover:from-blue-600 hover:to-purple-700 rounded-lg font-semibold transition">
            Upload Photo
          </button>
        </div>

        <!-- Filters -->
        <div class="flex gap-4 flex-wrap">
          <button class="px-4 py-2 rounded-lg bg-blue-600 text-white font-medium">All</button>
          <button class="px-4 py-2 rounded-lg bg-slate-700 hover:bg-slate-600 text-slate-300 font-medium transition">Public</button>
          <button class="px-4 py-2 rounded-lg bg-slate-700 hover:bg-slate-600 text-slate-300 font-medium transition">Private</button>
        </div>

        <!-- Gallery Grid -->
        <div *ngIf="!isLoading && gallery.length > 0" class="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-5 gap-4">
          <div *ngFor="let item of gallery" class="group relative rounded-lg overflow-hidden aspect-square cursor-pointer">
            <!-- Thumbnail -->
            <div class="w-full h-full bg-gradient-to-br from-blue-600/30 to-purple-600/30 flex items-center justify-center text-4xl group-hover:from-blue-600/50 group-hover:to-purple-600/50 transition">
              🖼️
            </div>

            <!-- Overlay -->
            <div class="absolute inset-0 bg-black/0 group-hover:bg-black/40 transition flex items-end justify-between p-3">
              <div class="opacity-0 group-hover:opacity-100 transition">
                <p class="text-sm font-semibold truncate">{{ item.originalFileName }}</p>
                <p class="text-xs text-slate-300">{{ item.uploadedAt | date:'short' }}</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Loading State -->
        <div *ngIf="isLoading" class="text-center py-16">
          <p class="text-slate-400">Loading gallery...</p>
        </div>

        <!-- Empty State -->
        <div *ngIf="!isLoading && gallery.length === 0" class="text-center py-16 bg-slate-800/20 border border-slate-700/50 rounded-lg">
          <div class="text-5xl mb-4">📷</div>
          <h3 class="text-2xl font-bold mb-2">No Photos Yet</h3>
          <p class="text-slate-400 mb-6">Start sharing your moments</p>
          <button class="px-6 py-3 bg-blue-600 hover:bg-blue-700 rounded-lg font-semibold transition">
            Upload Your First Photo
          </button>
        </div>
      </div>
    </app-layout>
  `,
})
export class GalleryComponent implements OnInit {
  private api = inject(SangrahApiService);

  gallery: MediaItem[] = [];
  isLoading = true;

  ngOnInit(): void {
    this.loadGallery();
  }

  loadGallery(): void {
    this.api.listGalleryMedia().subscribe({
      next: (response) => {
        this.gallery = response.items;
        this.isLoading = false;
      },
      error: () => {
        this.gallery = [];
        this.isLoading = false;
      },
    });
  }
}

