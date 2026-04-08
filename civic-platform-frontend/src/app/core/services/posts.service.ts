import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export enum PostType {
  EVENT_ANNOUNCEMENT = 'EVENT_ANNOUNCEMENT',
  TESTIMONIAL = 'TESTIMONIAL',
  STATUS = 'STATUS',
  CAMPAIGN_ANNOUNCEMENT = 'CAMPAIGN_ANNOUNCEMENT'
}

export enum PostStatus {
  PENDING = 'PENDING',
  ACCEPTED = 'ACCEPTED',
  REJECTED = 'REJECTED'
}

export interface Post {
  id: number;
  content: string;
  type: PostType;
  status: PostStatus;
  creator: string;
  createdAt: string;
  likesCount: number;
  campaignId?: number;
  campaignName?: string;
  comments?: Comment[];
}

export interface Comment {
  id: number;
  content: string;
  authorName: string;
  authorEmail?: string;
  authorId?: number;
  postId?: number;
  createdAt: string;
}

export interface PostRequest {
  content: string;
  type: PostType;
  status?: PostStatus;
}

export interface CommentRequest {
  postId: number;
  content: string;
}

@Injectable({ providedIn: 'root' })
export class PostsService {
  private readonly API_URL = 'http://localhost:8081/api/posts';

  constructor(private http: HttpClient) {}

  getAllPosts(): Observable<Post[]> {
    return this.http.get<Post[]>(this.API_URL);
  }

  getMyPosts(): Observable<Post[]> {
    return this.http.get<Post[]>(`${this.API_URL}/my`);
  }

  getPostById(id: number): Observable<Post> {
    return this.http.get<Post>(`${this.API_URL}/${id}`);
  }

  createPost(postData: PostRequest): Observable<Post> {
    return this.http.post<Post>(this.API_URL, postData);
  }

  updatePost(id: number, postData: Partial<PostRequest>): Observable<Post> {
    return this.http.put<Post>(`${this.API_URL}/${id}`, postData);
  }

  deletePost(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API_URL}/${id}`);
  }

  getCommentsByPost(postId: number): Observable<Comment[]> {
    return this.http.get<Comment[]>(`http://localhost:8081/api/comments/post/${postId}`);
  }

  createComment(commentData: CommentRequest): Observable<Comment> {
    return this.http.post<Comment>('http://localhost:8081/api/comments', commentData);
  }

  likePost(postId: number): Observable<void> {
    return this.http.post<void>(`http://localhost:8081/api/likes/posts/${postId}`, {});
  }

  unlikePost(postId: number): Observable<void> {
    return this.http.delete<void>(`http://localhost:8081/api/likes/posts/${postId}`);
  }

  checkLike(postId: number): Observable<boolean> {
    return this.http.get<boolean>(`http://localhost:8081/api/likes/posts/${postId}/check`);
  }
}
