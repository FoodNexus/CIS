import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { PostsService, Post } from '@core/services/posts.service';

@Component({
  selector: 'app-posts',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './posts.component.html',
  styleUrls: ['./posts.component.scss']
})
export class PostsComponent implements OnInit {
  posts: Post[] = [];
  isLoading = false;
  errorMessage = '';
  likedPosts = new Set<number>();

  constructor(private postsService: PostsService) {}

  ngOnInit(): void {
    this.loadPosts();
  }

  loadPosts(): void {
    this.isLoading = true;
    this.errorMessage = '';
    this.postsService.getAllPosts().subscribe({
      next: (posts) => {
        this.posts = posts;
        this.isLoading = false;
        this.initLikeStates(posts);
      },
      error: (error) => {
        this.errorMessage = error.status === 0
          ? 'Cannot connect to server. Please make sure the backend is running.'
          : (error.error?.message || 'Failed to load posts');
        this.isLoading = false;
      }
    });
  }

  private initLikeStates(posts: Post[]): void {
    posts.forEach(post => {
      this.postsService.checkLike(post.id).subscribe({
        next: (liked) => { if (liked) this.likedPosts.add(post.id); },
        error: () => {}
      });
    });
  }

  toggleLike(post: Post): void {
    if (this.likedPosts.has(post.id)) {
      this.postsService.unlikePost(post.id).subscribe({
        next: () => {
          post.likesCount = Math.max(0, post.likesCount - 1);
          this.likedPosts.delete(post.id);
        }
      });
    } else {
      this.postsService.likePost(post.id).subscribe({
        next: () => {
          post.likesCount++;
          this.likedPosts.add(post.id);
        }
      });
    }
  }

  isLiked(postId: number): boolean {
    return this.likedPosts.has(postId);
  }

  formatType(type: string): string {
    return type.replace(/_/g, ' ');
  }

  getTypeColor(type: string): string {
    switch (type) {
      case 'EVENT_ANNOUNCEMENT':    return 'bg-blue-100 text-blue-800';
      case 'CAMPAIGN_ANNOUNCEMENT': return 'bg-green-100 text-green-800';
      case 'TESTIMONIAL':           return 'bg-purple-100 text-purple-800';
      case 'STATUS':                return 'bg-gray-100 text-gray-700';
      default:                      return 'bg-gray-100 text-gray-700';
    }
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'ACCEPTED': return 'bg-emerald-100 text-emerald-800';
      case 'PENDING':  return 'bg-yellow-100 text-yellow-800';
      case 'REJECTED': return 'bg-red-100 text-red-800';
      default:         return 'bg-gray-100 text-gray-700';
    }
  }
}
