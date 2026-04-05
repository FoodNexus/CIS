import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export enum PostType {
  ANNONCE = 'ANNONCE',
  EVENT = 'EVENT',
  SUCCESS_STORY = 'SUCCESS_STORY'
}

export enum PostStatus {
  DRAFT = 'DRAFT',
  PUBLISHED = 'PUBLISHED',
  ARCHIVED = 'ARCHIVED'
}

export interface Post {
  id: number;
  content: string;
  type: PostType;
  status: PostStatus;
  creator: string;
  createdAt: string;
  likesCount: number;
  commentsCount: number;
}

export interface Comment {
  id: number;
  content: string;
  authorName: string;
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
  private readonly API_URL = 'http://localhost:8180/api/posts';

  constructor(private http: HttpClient) {}

  getAllPosts(): Observable<Post[]> {
    return this.http.get<Post[]>(this.API_URL);
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
    return this.http.get<Comment[]>(`/api/comments/post/${postId}`);
  }

  createComment(commentData: CommentRequest): Observable<Comment> {
    return this.http.post<Comment>('/api/comments', commentData);
  }

  likePost(postId: number): Observable<void> {
    return this.http.post<void>(`/api/likes/posts/${postId}`, {});
  }
}
