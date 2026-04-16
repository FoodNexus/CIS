import { Campaign } from '@core/services/campaigns.service';
import { Event } from '@core/services/events.service';
import { Post } from '@core/services/posts.service';
import { Project } from '@core/services/projects.service';

/**
 * Response from GET /recommendations/feed (ML-powered personalized content).
 */
export interface FeedResponse {
  campaigns: Campaign[];
  projects: Project[];
  posts: Post[];
  events: Event[];
  modelVersion?: string;
  coldStart?: boolean;
}
