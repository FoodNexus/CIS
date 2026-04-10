import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-composer-toolbar',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './composer-toolbar.component.html',
  styleUrls: ['./composer-toolbar.component.scss']
})
export class ComposerToolbarComponent {
  @Output() insertText = new EventEmitter<string>();
  @Output() filesPicked = new EventEmitter<File[]>();

  @Input() maxFiles = 10;
  @Input() disabled = false;

  emojiOpen = false;
  gifOpen = false;
  gifQuery = '';
  gifResults: { id: string; url: string; preview: string }[] = [];
  gifLoading = false;
  gifError = '';
  pasteGifUrl = '';

  readonly giphyKey = environment.giphyApiKey ?? '';

  toggleEmoji(): void {
    this.emojiOpen = !this.emojiOpen;
    if (this.emojiOpen) {
      this.gifOpen = false;
    }
  }

  toggleGif(): void {
    this.gifOpen = !this.gifOpen;
    if (this.gifOpen) {
      this.emojiOpen = false;
    }
  }

  pickEmoji(e: string): void {
    this.insertText.emit(e);
    this.emojiOpen = false;
  }

  onGifSearch(): void {
    const q = this.gifQuery.trim();
    if (!q || !this.giphyKey) {
      return;
    }
    this.gifLoading = true;
    this.gifError = '';
    this.gifResults = [];
    const url =
      `https://api.giphy.com/v1/gifs/search?api_key=${encodeURIComponent(this.giphyKey)}` +
      `&q=${encodeURIComponent(q)}&limit=12&rating=g`;
    fetch(url)
      .then((r) => r.json())
      .then((data) => {
        this.gifLoading = false;
        const list = (data?.data ?? []) as { id: string; images: { fixed_height?: { url: string }; downsized?: { url: string } } }[];
        this.gifResults = list.map((g) => ({
          id: g.id,
          url: g.images?.fixed_height?.url || g.images?.downsized?.url || '',
          preview: g.images?.fixed_height?.url || g.images?.downsized?.url || ''
        })).filter((g) => g.url);
      })
      .catch(() => {
        this.gifLoading = false;
        this.gifError = 'GIF search failed.';
      });
  }

  selectGif(url: string): void {
    if (url) {
      this.insertText.emit('\n' + url + '\n');
    }
    this.gifOpen = false;
    this.gifResults = [];
    this.gifQuery = '';
  }

  insertPastedGif(): void {
    const u = this.pasteGifUrl.trim();
    if (!u) {
      return;
    }
    this.insertText.emit('\n' + u + '\n');
    this.pasteGifUrl = '';
    this.gifOpen = false;
  }

  onFileInput(ev: Event): void {
    const input = ev.target as HTMLInputElement;
    const files = input.files;
    if (!files?.length) {
      return;
    }
    const arr = Array.from(files).slice(0, this.maxFiles);
    this.filesPicked.emit(arr);
    input.value = '';
  }

  /** Popular Unicode emoji subset (no external lib). */
  readonly emojiPalette = [
    '😀', '😃', '😄', '😁', '😅', '😂', '🤣', '😊', '😇', '🙂', '😉', '😍',
    '🥰', '😘', '😗', '😋', '😛', '😜', '🤪', '😎', '🥳', '🤩', '😢', '😭',
    '😤', '😠', '🤔', '🤨', '😐', '😶', '🙄', '😴', '🤤', '😮', '😱', '🤗',
    '👍', '👎', '👏', '🙌', '👋', '🤝', '🙏', '💪', '✨', '🔥', '💯', '❤️',
    '🧡', '💛', '💚', '💙', '💜', '🖤', '💔', '❣️', '💕', '💞', '💤', '🎉',
    '✅', '❌', '⭐', '🌟', '☀️', '🌈', '⚡', '🎵', '📌', '📎', '🔗', '💬'
  ];
}
