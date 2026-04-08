import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { PostsService, PostType } from '@core/services/posts.service';

@Component({
  selector: 'app-post-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  template: `
    <div class="max-w-2xl mx-auto p-6">
      <h2 class="text-2xl font-bold mb-6">Create New Post</h2>
      <form [formGroup]="postForm" (ngSubmit)="onSubmit()" class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-gray-700">Type *</label>
          <select formControlName="type" class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm border px-3 py-2">
            <option *ngFor="let t of postTypes" [value]="t">{{ formatType(t) }}</option>
          </select>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-700">Content *</label>
          <textarea formControlName="content" rows="6" placeholder="What's on your mind?" class="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-indigo-500 focus:ring-indigo-500 sm:text-sm border px-3 py-2"></textarea>
          <p *ngIf="postForm.get('content')?.touched && postForm.get('content')?.hasError('required')" class="mt-1 text-sm text-red-600">Content is required.</p>
        </div>
        <p *ngIf="errorMessage" class="text-sm text-red-600">{{ errorMessage }}</p>
        <div class="flex justify-end space-x-3 pt-4">
          <button type="button" (click)="router.navigate(['/posts'])" class="px-4 py-2 border border-gray-300 rounded-md text-sm font-medium text-gray-700 hover:bg-gray-50">Cancel</button>
          <button type="submit" [disabled]="postForm.invalid || isLoading" class="px-4 py-2 border border-transparent rounded-md shadow-sm text-sm font-medium text-white bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50">
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

  constructor(
    private fb: FormBuilder,
    public router: Router,
    private postsService: PostsService
  ) {
    this.postForm = this.fb.group({
      content: ['', Validators.required],
      type: [PostType.STATUS, Validators.required]
    });
  }

  formatType(type: string): string {
    return type.replace(/_/g, ' ');
  }

  onSubmit(): void {
    if (this.postForm.invalid) {
      this.postForm.markAllAsTouched();
      return;
    }
    this.isLoading = true;
    this.errorMessage = '';
    this.postsService.createPost({
      content: this.postForm.value.content,
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
