import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import {
  FormBuilder,
  FormGroup,
  ReactiveFormsModule,
  Validators
} from '@angular/forms';
import { FormsModule } from '@angular/forms';
import {
  Comment,
  Post,
  PostStatus,
  PostType,
  PostsService
} from '@core/services/posts.service';

@Component({
  selector: 'app-my-posts',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule],
  templateUrl: './my-posts.component.html',
  styleUrls: ['./my-posts.component.scss']
})
export class MyPostsComponent implements OnInit {
  posts: Post[] = [];
  isLoading = false;
  errorMessage = '';
  successMessage = '';
  typeFilter: PostType | '' = '';
  statusFilter: PostStatus | '' = '';
  showModal = false;
  editingPost: Post | null = null;
  postForm: FormGroup;
  submitLoading = false;
  showDeleteModal = false;
  postToDelete: Post | null = null;
  deleteLoading = false;
  expandedPostId: number | null = null;
  commentsByPostId: Record<number, Comment[]> = {};
  commentsLoading: Record<number, boolean> = {};
  newCommentText: Record<number, string> = {};
  commentSubmitLoading: Record<number, boolean> = {};
  removingId: number | null = null;

  readonly postTypes = Object.values(PostType);
  readonly postStatuses = Object.values(PostStatus);

  constructor(
    private postsService: PostsService,
    private fb: FormBuilder
  ) {
    this.postForm = this.fb.group({
      type: [PostType.STATUS, [Validators.required]],
      content: [
        '',
        [Validators.required, Validators.minLength(10), Validators.maxLength(1000)]
      ]
    });
  }

  ngOnInit(): void {
    this.loadPosts();
  }

  get filteredPosts(): Post[] {
    let list = [...this.posts];
    if (this.typeFilter) {
      list = list.filter((p) => p.type === this.typeFilter);
    }
    if (this.statusFilter) {
      list = list.filter((p) => p.status === this.statusFilter);
    }
    return list;
  }

  get contentLength(): number {
    const v = this.postForm.get('content')?.value;
    return v ? String(v).length : 0;
  }

  loadPosts(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.postsService.getMyPosts().subscribe({
      next: (posts) => {
        this.posts = posts.sort(
          (a, b) =>
            new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
        );
        this.isLoading = false;
      },
      error: (e) => {
        this.errorMessage = e.error?.message || 'Impossible de charger vos publications';
        this.isLoading = false;
      }
    });
  }

  openCreateModal(): void {
    this.editingPost = null;
    this.postForm.reset({
      type: PostType.STATUS,
      content: ''
    });
    this.showModal = true;
  }

  openEditModal(post: Post): void {
    this.editingPost = post;
    this.postForm.patchValue({
      type: post.type,
      content: post.content
    });
    this.showModal = true;
  }

  closeModal(): void {
    this.showModal = false;
    this.editingPost = null;
  }

  submitPost(): void {
    if (this.postForm.invalid) {
      this.postForm.markAllAsTouched();
      return;
    }
    this.submitLoading = true;
    const { type, content } = this.postForm.value;
    if (this.editingPost) {
      this.postsService
        .updatePost(this.editingPost.id, { type, content })
        .subscribe({
          next: (updated) => {
            const idx = this.posts.findIndex((p) => p.id === updated.id);
            if (idx >= 0) {
              this.posts[idx] = updated;
              this.posts = [...this.posts];
            }
            this.successMessage = 'Publication mise à jour.';
            this.submitLoading = false;
            this.closeModal();
            setTimeout(() => (this.successMessage = ''), 4000);
          },
          error: (e) => {
            this.errorMessage = e.error?.message || 'Échec de la mise à jour';
            this.submitLoading = false;
          }
        });
    } else {
      this.postsService.createPost({ type, content }).subscribe({
        next: (created) => {
          this.posts = [created, ...this.posts];
          this.successMessage = 'Publication créée.';
          this.submitLoading = false;
          this.closeModal();
          setTimeout(() => (this.successMessage = ''), 4000);
        },
        error: (e) => {
          this.errorMessage = e.error?.message || 'Échec de la création';
          this.submitLoading = false;
        }
      });
    }
  }

  openDeleteModal(post: Post): void {
    this.postToDelete = post;
    this.showDeleteModal = true;
  }

  cancelDelete(): void {
    this.showDeleteModal = false;
    this.postToDelete = null;
  }

  confirmDelete(): void {
    if (!this.postToDelete) {
      return;
    }
    const id = this.postToDelete.id;
    this.deleteLoading = true;
    this.removingId = id;
    this.postsService.deletePost(id).subscribe({
      next: () => {
        setTimeout(() => {
          this.posts = this.posts.filter((p) => p.id !== id);
          this.deleteLoading = false;
          this.showDeleteModal = false;
          this.postToDelete = null;
          this.removingId = null;
          this.successMessage = 'Publication supprimée.';
          setTimeout(() => (this.successMessage = ''), 4000);
        }, 280);
      },
      error: (e) => {
        this.errorMessage = e.error?.message || 'Suppression impossible';
        this.deleteLoading = false;
        this.removingId = null;
        this.showDeleteModal = false;
      }
    });
  }

  toggleComments(post: Post): void {
    if (this.expandedPostId === post.id) {
      this.expandedPostId = null;
      return;
    }
    this.expandedPostId = post.id;
    if (!this.commentsByPostId[post.id]) {
      if (post.comments && post.comments.length > 0) {
        this.commentsByPostId[post.id] = post.comments;
        return;
      }
      this.commentsLoading[post.id] = true;
      this.postsService.getCommentsByPost(post.id).subscribe({
        next: (comments) => {
          this.commentsByPostId[post.id] = comments;
          this.commentsLoading[post.id] = false;
        },
        error: () => {
          this.commentsByPostId[post.id] = [];
          this.commentsLoading[post.id] = false;
        }
      });
    }
  }

  commentsCount(post: Post): number {
    const cached = this.commentsByPostId[post.id];
    if (cached) {
      return cached.length;
    }
    return post.comments?.length ?? 0;
  }

  submitComment(postId: number): void {
    const text = (this.newCommentText[postId] || '').trim();
    if (!text) {
      return;
    }
    this.commentSubmitLoading[postId] = true;
    this.postsService.createComment({ postId, content: text }).subscribe({
      next: (c) => {
        const list = this.commentsByPostId[postId] || [];
        this.commentsByPostId[postId] = [...list, c];
        this.newCommentText[postId] = '';
        this.commentSubmitLoading[postId] = false;
      },
      error: () => {
        this.commentSubmitLoading[postId] = false;
      }
    });
  }

  formatRelative(iso: string): string {
    const d = new Date(iso);
    const now = new Date();
    const sec = Math.floor((now.getTime() - d.getTime()) / 1000);
    if (sec < 60) {
      return "à l'instant";
    }
    const min = Math.floor(sec / 60);
    if (min < 60) {
      return `il y a ${min} min`;
    }
    const h = Math.floor(min / 60);
    if (h < 24) {
      return `il y a ${h} h`;
    }
    const days = Math.floor(h / 24);
    if (days < 7) {
      return `il y a ${days} j`;
    }
    return d.toLocaleDateString('fr-FR');
  }

  getTypePillClass(type: PostType): string {
    switch (type) {
      case PostType.EVENT_ANNOUNCEMENT:
        return 'bg-blue-100 text-blue-800';
      case PostType.TESTIMONIAL:
        return 'bg-emerald-100 text-emerald-800';
      case PostType.STATUS:
        return 'bg-teal-100 text-teal-800';
      case PostType.CAMPAIGN_ANNOUNCEMENT:
        return 'bg-amber-100 text-amber-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  }

  getStatusPillClass(status: PostStatus): string {
    switch (status) {
      case PostStatus.PENDING:
        return 'bg-amber-100 text-amber-800';
      case PostStatus.ACCEPTED:
        return 'bg-green-100 text-green-800';
      case PostStatus.REJECTED:
        return 'bg-red-100 text-red-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  }
}
