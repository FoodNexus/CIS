import { Directive, HostBinding, HostListener, Input } from '@angular/core';
import { ImageViewerService } from '@core/services/image-viewer.service';

/** Click to open fullscreen zoom viewer (images only). */
@Directive({
  selector: '[appZoomableImage]',
  standalone: true
})
export class ZoomableImageDirective {
  /** Image URL to show in the viewer (usually same as img src). */
  @Input({ required: true }) appZoomableImage!: string;

  @HostBinding('class') hostClass = 'cursor-pointer transition-opacity hover:opacity-95';

  constructor(private imageViewer: ImageViewerService) {}

  @HostListener('click', ['$event'])
  onClick(ev: Event): void {
    ev.preventDefault();
    ev.stopPropagation();
    this.imageViewer.open(this.appZoomableImage);
  }
}
