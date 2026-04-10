import { Component, Input, OnChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ZoomableImageDirective } from '@shared/directives/zoomable-image.directive';

export type RichSegment = { type: 'text' | 'img'; value: string };

@Component({
  selector: 'app-rich-content',
  standalone: true,
  imports: [CommonModule, ZoomableImageDirective],
  template: `
    <div class="rich-content text-gray-800 leading-relaxed">
      <ng-container *ngFor="let line of lineGroups">
        <div class="min-h-[1em]">
          <ng-container *ngFor="let seg of line">
            <img *ngIf="seg.type === 'img'"
                 [src]="seg.value"
                 [appZoomableImage]="seg.value"
                 alt=""
                 class="inline-block max-w-full max-h-72 rounded-lg object-contain my-1 border border-gray-100"
                 loading="lazy" />
            <span *ngIf="seg.type === 'text'" class="whitespace-pre-wrap break-words">{{ seg.value }}</span>
          </ng-container>
        </div>
      </ng-container>
    </div>
  `
})
export class RichContentComponent implements OnChanges {
  @Input() content: string | null | undefined = '';

  lineGroups: RichSegment[][] = [];

  ngOnChanges(): void {
    this.lineGroups = this.parse(this.content ?? '');
  }

  /** Split by newlines; embed standalone image/GIF URLs as &lt;img&gt; (safe allowlist). */
  private parse(text: string): RichSegment[][] {
    if (!text) {
      return [];
    }
    return text.split('\n').map((line) => this.parseLine(line));
  }

  private parseLine(line: string): RichSegment[] {
    const t = line.trim();
    if (this.isEmbeddableImageUrl(t)) {
      return [{ type: 'img', value: t }];
    }
    return [{ type: 'text', value: line }];
  }

  private isEmbeddableImageUrl(s: string): boolean {
    if (!/^https?:\/\//i.test(s)) {
      return false;
    }
    if (/\.(gif|png|jpe?g|webp)(\?.*)?$/i.test(s)) {
      return true;
    }
    if (/i\.giphy\.com|media\.giphy\.com|media\d?\.tenor\.com|cdn\.discordapp\.com\/attachments/i.test(s)) {
      return true;
    }
    return false;
  }
}
