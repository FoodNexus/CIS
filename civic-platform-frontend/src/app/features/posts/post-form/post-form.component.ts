import { Component, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { PostsService, PostType } from '@core/services/posts.service';
import { ComposerToolbarComponent } from '@shared/components/composer-toolbar/composer-toolbar.component';

@Component({
  selector: 'app-post-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, ComposerToolbarComponent],
  template: `
    <div class="max-w-2xl mx-auto p-6">
      <h2 class="text-2xl font-bold mb-6">Create New Post</h2>
      <form [formGroup]="postForm" (ngSubmit)="onSubmit()" class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-gray-700">Type *</label>
          <select formControlName="type" class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-emerald-500 focus:ring-emerald-500 sm:text-sm border px-3 py-2">
            <option *ngFor="let t of postTypes" [value]="t">{{ formatType(t) }}</option>
          </select>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700">Content</label>
          <textarea #bodyTextarea formControlName="content" rows="6" placeholder="What's on your mind? Emoji, GIF links, and media attachments below."
                    class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-emerald-500 focus:ring-emerald-500 sm:text-sm border px-3 py-2"></textarea>
          <p class="mt-2 text-xs text-gray-500">Use the toolbar for emoji, GIF search (optional Giphy key), or attach images/videos.</p>
        </div>

        <app-composer-toolbar
          [disabled]="isLoading"
          [maxFiles]="10"
          (insertText)="insertAtCursor($event)"
          (filesPicked)="onMediaFiles($event)">
        </app-composer-toolbar>

        <div *ngIf="selectedFiles.length" class="flex flex-wrap gap-2">
          <span *ngFor="let f of selectedFiles; let i = index"
                class="inline-flex items-center gap-1 px-2 py-1 rounded-lg bg-emerald-50 text-emerald-900 text-xs border border-emerald-100">
            {{ f.name }}
            <button type="button" (click)="removeFile(i)" class="text-emerald-700 hover:text-emerald-900 font-bold">×</button>
          </span>
        </div>

        <p *ngIf="errorMessage" class="text-sm text-red-600">{{ errorMessage }}</p>
        <div class="flex justify-end space-x-3 pt-4">
          <button type="button" (click)="router.navigate(['/posts'])" class="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 hover:bg-gray-50">Cancel</button>
          <button type="submit" [disabled]="postForm.invalid || isLoading" class="px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-emerald-600 hover:bg-emerald-700 disabled:opacity-50">
            {{ isLoading ? 'Posting...' : 'Create Post' }}
          </button>
        </div>
      </form>
    </div>
  `
})
export class PostFormComponent {
  postForm: FormGroup;
  postTypes = Object.values(PostType);
  isLoading = false;
  errorMessage = '';
  selectedFiles: File[] = [];

  @ViewChild('bodyTextarea') bodyTextarea?: ElementRef<HTMLTextAreaElement>;

  constructor(
    private fb: FormBuilder,
    public router: Router,
    private postsService: PostsService
  ) {
    this.postForm = this.fb.group({
      content: [''],
      type: [PostType.STATUS, Validators.required]
    });
  }

  formatType(type: string): string {
    return type.replace(/_/g, ' ');
  }

  insertAtCursor(text: string): void {
    const el = this.bodyTextarea?.nativeElement;
    const cur = this.postForm.controls['content'].value ?? '';
    if (!el) {
      this.postForm.patchValue({ content: cur + text });
      return;
    }
    const start = el.selectionStart ?? cur.length;
    const end = el.selectionEnd ?? cur.length;
    const next = cur.slice(0, start) + text + cur.slice(end);
    this.postForm.patchValue({ content: next });
    setTimeout(() => {
      el.focus();
      const pos = start + text.length;
      el.setSelectionRange(pos, pos);
    });
  }

  onMediaFiles(files: File[]): void {
    const merged = [...this.selectedFiles, ...files].slice(0, 10);
    this.selectedFiles = merged;
  }

  removeFile(index: number): void {
    this.selectedFiles = this.selectedFiles.filter((_, i) => i !== index);
  }

  onSubmit(): void {
    if (this.postForm.invalid) {
      this.postForm.markAllAsTouched();
      return;
    }
    const text = (this.postForm.value.content ?? '').trim();
    if (!text && this.selectedFiles.length === 0) {
      this.errorMessage = 'Add some text or at least one media file.';
      return;
    }
    this.isLoading = true;
    this.errorMessage = '';

    if (this.selectedFiles.length > 0) {
      const fd = new FormData();
      fd.append('content', this.postForm.value.content ?? '');
      fd.append('type', this.postForm.value.type);
      this.selectedFiles.forEach((f) => fd.append('files', f, f.name));
      this.postsService.createPostMultipart(fd).subscribe({
        next: () => this.router.navigate(['/posts']),
        error: (err) => {
          this.errorMessage = err?.error?.message || 'Failed to create post. Please try again.';
          this.isLoading = false;
        }
      });
    } else {
      this.postsService.createPost({
        content: text,
        type: this.postForm.value.type
      }).subscribe({
        next: () => this.router.navigate(['/posts']),
        error: (err) => {
          this.errorMessage = err?.error?.message || 'Failed to create post. Please try again.';
          this.isLoading = false;
        }
      });
    }
  }
}
