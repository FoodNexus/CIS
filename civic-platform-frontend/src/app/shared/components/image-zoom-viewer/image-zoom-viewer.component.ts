import {
  Component,
  HostListener,
  Input,
  OnChanges,
  SimpleChanges,
  ElementRef,
  ViewChild
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ImageViewerService } from '@core/services/image-viewer.service';

@Component({
  selector: 'app-image-zoom-viewer',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './image-zoom-viewer.component.html',
  styleUrls: ['./image-zoom-viewer.component.scss']
})
export class ImageZoomViewerComponent implements OnChanges {
  @Input({ required: true }) src!: string;

  @ViewChild('imgEl') imgEl?: ElementRef<HTMLImageElement>;

  scale = 1;
  panX = 0;
  panY = 0;
  private panning = false;
  private startX = 0;
  private startY = 0;
  private originPanX = 0;
  private originPanY = 0;

  constructor(private imageViewer: ImageViewerService) {}

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['src'] && this.src) {
      this.resetView();
    }
  }

  get transform(): string {
    return `translate(${this.panX}px, ${this.panY}px) scale(${this.scale})`;
  }

  resetView(): void {
    this.scale = 1;
    this.panX = 0;
    this.panY = 0;
  }

  zoomIn(): void {
    this.scale = Math.min(6, Math.round((this.scale + 0.25) * 100) / 100);
  }

  zoomOut(): void {
    this.scale = Math.max(0.25, Math.round((this.scale - 0.25) * 100) / 100);
    if (this.scale <= 1) {
      this.panX = 0;
      this.panY = 0;
    }
  }

  onWheel(ev: WheelEvent): void {
    ev.preventDefault();
    const delta = ev.deltaY > 0 ? -0.12 : 0.12;
    this.scale = Math.min(6, Math.max(0.25, Math.round((this.scale + delta) * 100) / 100));
    if (this.scale <= 1) {
      this.panX = 0;
      this.panY = 0;
    }
  }

  onPanStart(ev: MouseEvent | TouchEvent): void {
    if (this.scale <= 1) {
      return;
    }
    this.panning = true;
    const clientX = 'touches' in ev ? ev.touches[0].clientX : ev.clientX;
    const clientY = 'touches' in ev ? ev.touches[0].clientY : ev.clientY;
    this.startX = clientX;
    this.startY = clientY;
    this.originPanX = this.panX;
    this.originPanY = this.panY;
  }

  @HostListener('document:mousemove', ['$event'])
  onPanMove(ev: MouseEvent): void {
    if (!this.panning) {
      return;
    }
    this.panX = this.originPanX + (ev.clientX - this.startX);
    this.panY = this.originPanY + (ev.clientY - this.startY);
  }

  @HostListener('document:mouseup')
  onPanEnd(): void {
    this.panning = false;
  }

  @HostListener('document:touchmove', ['$event'])
  onTouchMove(ev: TouchEvent): void {
    if (!this.panning || ev.touches.length !== 1) {
      return;
    }
    ev.preventDefault();
    const clientX = ev.touches[0].clientX;
    const clientY = ev.touches[0].clientY;
    this.panX = this.originPanX + (clientX - this.startX);
    this.panY = this.originPanY + (clientY - this.startY);
  }

  @HostListener('document:touchend')
  onTouchEnd(): void {
    this.panning = false;
  }

  @HostListener('document:keydown', ['$event'])
  onKeydown(ev: KeyboardEvent): void {
    if (ev.key === 'Escape') {
      this.close();
    }
  }

  close(): void {
    this.imageViewer.close();
  }

  backdropClick(): void {
    this.close();
  }

  stopClose(ev: MouseEvent): void {
    ev.stopPropagation();
  }
}
