import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { PostsService, Post, Comment, CommentRequest } from '@core/services/posts.service';
import { FormBuilder, FormGroup, ReactiveFormsModule } from '@angular/forms';
import { RichContentComponent } from '@shared/components/rich-content/rich-content.component';
import { ComposerToolbarComponent } from '@shared/components/composer-toolbar/composer-toolbar.component';
import { ZoomableImageDirective } from '@shared/directives/zoomable-image.directive';

@Component({
  selector: 'app-post-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    ReactiveFormsModule,
    RichContentComponent,
    ComposerToolbarComponent,
    ZoomableImageDirective
  ],
  templateUrl: './post-detail.component.html',
  styleUrls: ['./post-detail.component.scss']
})
export class PostDetailComponent implements OnInit {
  post: Post | null = null;
  comments: Comment[] = [];
  isLoading = false;
  isSubmitting = false;
  isLiked = false;
  errorMessage = '';
  commentForm: FormGroup;
  commentFiles: File[] = [];

  @ViewChild('commentTextarea') commentTextarea?: ElementRef<HTMLTextAreaElement>;

  constructor(
    private route: ActivatedRoute,
    public postsService: PostsService,
    private fb: FormBuilder
  ) {
    this.commentForm = this.fb.group({
      content: ['']
    });
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadPost(Number(id));
      this.loadComments(Number(id));
      this.checkLiked(Number(id));
    }
  }

  mediaUrl(relative: string): string {
    return this.postsService.mediaAttachmentUrl(relative);
  }

  loadPost(id: number): void {
    this.isLoading = true;
    this.postsService.getPostById(id).subscribe({
      next: (post) => { this.post = post; this.isLoading = false; },
      error: (err) => {
        this.errorMessage = err.error?.message || 'Failed to load post';
        this.isLoading = false;
      }
    });
  }

  loadComments(postId: number): void {
    this.postsService.getCommentsByPost(postId).subscribe({
      next: (comments) => { this.comments = comments; },
      error: () => {}
    });
  }

  checkLiked(postId: number): void {
    this.postsService.checkLike(postId).subscribe({
      next: (liked) => { this.isLiked = liked; },
      error: () => {}
    });
  }

  toggleLike(): void {
    if (!this.post) return;
    if (this.isLiked) {
      this.postsService.unlikePost(this.post.id).subscribe({
        next: () => {
          this.post!.likesCount = Math.max(0, this.post!.likesCount - 1);
          this.isLiked = false;
        }
      });
    } else {
      this.postsService.likePost(this.post.id).subscribe({
        next: () => {
          this.post!.likesCount++;
          this.isLiked = true;
        }
      });
    }
  }

  insertInComment(text: string): void {
    const el = this.commentTextarea?.nativeElement;
    const cur = this.commentForm.controls['content'].value ?? '';
    if (!el) {
      this.commentForm.patchValue({ content: cur + text });
      return;
    }
    const start = el.selectionStart ?? cur.length;
    const end = el.selectionEnd ?? cur.length;
    const next = cur.slice(0, start) + text + cur.slice(end);
    this.commentForm.patchValue({ content: next });
    setTimeout(() => {
      el.focus();
      const pos = start + text.length;
      el.setSelectionRange(pos, pos);
    });
  }

  onCommentFiles(files: File[]): void {
    this.commentFiles = [...this.commentFiles, ...files].slice(0, 10);
  }

  removeCommentFile(i: number): void {
    this.commentFiles = this.commentFiles.filter((_, idx) => idx !== i);
  }

  addComment(): void {
    if (!this.post) return;
    const text = (this.commentForm.value.content ?? '').trim();
    if (!text && this.commentFiles.length === 0) {
      return;
    }
    this.isSubmitting = true;
    this.errorMessage = '';

    if (this.commentFiles.length > 0) {
      const fd = new FormData();
      fd.append('content', this.commentForm.value.content ?? '');
      fd.append('postId', String(this.post.id));
      this.commentFiles.forEach((f) => fd.append('files', f, f.name));
      this.postsService.createCommentMultipart(fd).subscribe({
        next: () => {
          this.commentForm.reset();
          this.commentFiles = [];
          this.isSubmitting = false;
          this.loadComments(this.post!.id);
        },
        error: (err) => {
          this.errorMessage = err.error?.message || 'Failed to add comment';
          this.isSubmitting = false;
        }
      });
    } else {
      const commentRequest: CommentRequest = {
        content: text,
        postId: this.post.id
      };
      this.postsService.createComment(commentRequest).subscribe({
        next: () => {
          this.commentForm.reset();
          this.isSubmitting = false;
          this.loadComments(this.post!.id);
        },
        error: (err) => {
          this.errorMessage = err.error?.message || 'Failed to add comment';
          this.isSubmitting = false;
        }
      });
    }
  }

  formatType(type: string): string {
    return type?.replace(/_/g, ' ') || '';
  }

  getTypeColor(type: string): string {
    switch (type) {
      case 'EVENT_ANNOUNCEMENT':    return 'bg-emerald-100 text-emerald-800';
      case 'CAMPAIGN_ANNOUNCEMENT': return 'bg-green-100 text-green-800';
      case 'TESTIMONIAL':           return 'bg-emerald-100 text-emerald-800';
      default:                      return 'bg-gray-100 text-gray-700';
    }
  }
}
