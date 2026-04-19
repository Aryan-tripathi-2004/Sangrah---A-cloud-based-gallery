import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SangrahApiService, MediaItem } from '../../core/api/sangrah-api.service';
import { LayoutComponent } from '../../shared/layout/layout.component';

@Component({
  selector: 'app-gallery',
  standalone: true,
  imports: [CommonModule, LayoutComponent],
  templateUrl: './gallery.component.html',
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

