import { Component, ChangeDetectionStrategy, input, output, signal, effect } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-avatar-upload',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [CommonModule],
  templateUrl: './avatar-upload.component.html',
})
export class AvatarUploadComponent {
  avatarUrl = input<string | null | undefined>(null);
  displayName = input<string>('User');
  isUploading = input<boolean>(false);

  fileSelected = output<File>();

  hasLoadError = signal<boolean>(false);

  constructor() {
    effect(() => {
      // Whenever avatarUrl changes, reset the error state to allow new image to load
      this.avatarUrl();
      this.hasLoadError.set(false);
    });
  }

  onImageError(): void {
    this.hasLoadError.set(true);
  }

  onImageLoad(): void {
    this.hasLoadError.set(false);
  }

  onFileChange(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      this.hasLoadError.set(false);
      this.fileSelected.emit(input.files[0]);
      input.value = '';
    }
  }
}
