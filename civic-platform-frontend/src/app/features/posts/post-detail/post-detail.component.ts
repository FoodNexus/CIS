import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { PostsService, Post, Comment, CommentRequest } from '@core/services/posts.service';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';

@Component({
  selector: 'app-post-detail',
  standalone: true,
  imports: [CommonModule, RouterModule, ReactiveFormsModule],
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

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private postsService: PostsService,
    private fb: FormBuilder
  ) {
    this.commentForm = this.fb.group({
      content: ['', [Validators.required, Validators.minLength(1)]]
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

  addComment(): void {
    if (this.commentForm.invalid || !this.post) return;
    this.isSubmitting = true;

    const commentRequest: CommentRequest = {
      content: this.commentForm.value.content,
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
