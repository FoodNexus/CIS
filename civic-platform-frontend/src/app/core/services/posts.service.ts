import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

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

export interface MediaAttachment {
  id: number;
  kind: string;
  /** Relative path from API root, e.g. /posts/1/attachments/2 */
  url: string;
}

export interface Post {
  id: number;
  content: string | null;
  type: PostType;
  status: PostStatus;
  creator: string;
  createdAt: string;
  likesCount: number;
  campaignId?: number;
  campaignName?: string;
  comments?: Comment[];
  attachments?: MediaAttachment[];
}

export interface Comment {
  id: number;
  content: string | null;
  authorName: string;
  authorEmail?: string;
  authorId?: number;
  postId?: number;
  createdAt: string;
  attachments?: MediaAttachment[];
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
  private readonly postsUrl = `${environment.apiUrl}/posts`;
  private readonly commentsUrl = `${environment.apiUrl}/comments`;
  private readonly likesBase = `${environment.apiUrl}/likes`;

  constructor(private http: HttpClient) {}

  /** Full URL for streaming an attachment (images, GIF uploads, video). */
  mediaAttachmentUrl(relativeUrl: string): string {
    if (!relativeUrl) {
      return '';
    }
    if (relativeUrl.startsWith('http://') || relativeUrl.startsWith('https://')) {
      return relativeUrl;
    }
    const base = environment.apiUrl.replace(/\/$/, '');
    const path = relativeUrl.startsWith('/') ? relativeUrl : `/${relativeUrl}`;
    return `${base}${path}`;
  }

  getAllPosts(): Observable<Post[]> {
    return this.http.get<Post[]>(this.postsUrl);
  }

  getPostsByStatus(status: PostStatus): Observable<Post[]> {
    return this.http.get<Post[]>(`${this.postsUrl}/status/${status}`);
  }

  approvePost(id: number): Observable<Post> {
    return this.http.post<Post>(`${this.postsUrl}/${id}/approve`, {});
  }

  rejectPost(id: number): Observable<Post> {
    return this.http.post<Post>(`${this.postsUrl}/${id}/reject`, {});
  }

  getMyPosts(): Observable<Post[]> {
    return this.http.get<Post[]>(`${this.postsUrl}/my`);
  }

  getPostById(id: number): Observable<Post> {
    return this.http.get<Post>(`${this.postsUrl}/${id}`);
  }

  createPost(postData: PostRequest): Observable<Post> {
    return this.http.post<Post>(this.postsUrl, postData, {
      headers: { 'Content-Type': 'application/json' }
    });
  }

  createPostMultipart(formData: FormData): Observable<Post> {
    return this.http.post<Post>(this.postsUrl, formData);
  }

  updatePost(id: number, postData: Partial<PostRequest>): Observable<Post> {
    return this.http.put<Post>(`${this.postsUrl}/${id}`, postData);
  }

  deletePost(id: number): Observable<void> {
    return this.http.delete<void>(`${this.postsUrl}/${id}`);
  }

  getCommentsByPost(postId: number): Observable<Comment[]> {
    return this.http.get<Comment[]>(`${this.commentsUrl}/post/${postId}`);
  }

  createComment(commentData: CommentRequest): Observable<Comment> {
    return this.http.post<Comment>(this.commentsUrl, commentData, {
      headers: { 'Content-Type': 'application/json' }
    });
  }

  createCommentMultipart(formData: FormData): Observable<Comment> {
    return this.http.post<Comment>(this.commentsUrl, formData);
  }

  likePost(postId: number): Observable<void> {
    return this.http.post<void>(`${this.likesBase}/posts/${postId}`, {});
  }

  unlikePost(postId: number): Observable<void> {
    return this.http.delete<void>(`${this.likesBase}/posts/${postId}`);
  }

  checkLike(postId: number): Observable<boolean> {
    return this.http.get<boolean>(`${this.likesBase}/posts/${postId}/check`);
  }
}
