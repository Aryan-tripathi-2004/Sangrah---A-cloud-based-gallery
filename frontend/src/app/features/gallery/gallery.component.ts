import { Component, OnInit, inject, ChangeDetectionStrategy, signal } from '@angular/core';
import { SangrahApiService, MediaItem } from '../../core/api/sangrah-api.service';
import { LayoutComponent } from '../../shared/layout/layout.component';

@Component({
  selector: 'app-gallery',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [LayoutComponent],
  templateUrl: './gallery.component.html',
})
export class GalleryComponent implements OnInit {
  public readonly api = inject(SangrahApiService);

  gallery = signal<MediaItem[]>([]);
  isLoading = signal<boolean>(true);

  ngOnInit(): void {
    this.loadGallery();
  }

  loadGallery(): void {
    this.isLoading.set(true);
    this.api.listGalleryMedia().subscribe({
      next: (response) => {
        this.gallery.set(response.items || []);
        this.isLoading.set(false);
      },
      error: () => {
        this.gallery.set([]);
        this.isLoading.set(false);
      },
    });
  }
}
