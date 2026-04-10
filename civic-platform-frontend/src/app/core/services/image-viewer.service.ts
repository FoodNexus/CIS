import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';

/** Opens fullscreen image preview with zoom (used app-wide, including admin). */
@Injectable({ providedIn: 'root' })
export class ImageViewerService {
  private readonly urlSubject = new BehaviorSubject<string | null>(null);
  readonly url$ = this.urlSubject.asObservable();

  open(url: string): void {
    if (url) {
      this.urlSubject.next(url);
    }
  }

  close(): void {
    this.urlSubject.next(null);
  }
}
