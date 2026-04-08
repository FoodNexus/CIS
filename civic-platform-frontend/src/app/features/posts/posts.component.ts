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

  constructor(private postsService: PostsService) {}

  ngOnInit(): void {
    this.loadPosts();
  }

  loadPosts(): void {
    this.isLoading = true;
    this.postsService.getAllPosts().subscribe({
      next: (posts) => {
        this.posts = posts;
        this.isLoading = false;
      },
      error: (error) => {
        if (error.status === 0) {
          this.errorMessage = 'Cannot connect to server. Please make sure the backend is running.';
        } else {
          this.errorMessage = error.error?.message || 'Failed to load posts';
        }
        this.isLoading = false;
      }
    });
  }

  likePost(post: Post): void {
    this.postsService.likePost(post.id).subscribe({
      next: () => {
        post.likesCount++;
      },
      error: (error) => {
        this.errorMessage = error.error?.message || 'Failed to like post';
      }
    });
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'ACCEPTED': return 'bg-green-100 text-green-800';
      case 'PENDING': return 'bg-yellow-100 text-yellow-800';
      case 'REJECTED': return 'bg-red-100 text-red-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  }
}
