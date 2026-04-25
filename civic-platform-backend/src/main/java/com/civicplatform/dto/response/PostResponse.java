package com.civicplatform.dto.response;

import com.civicplatform.enums.PostStatus;
import com.civicplatform.enums.PostType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostResponse {
    
    private Long id;
    private String creator;
    private String content;
    private PostStatus status;
    private Integer likesCount;
    private PostType type;
    private LocalDateTime createdAt;
    private Long campaignId;
    private String campaignName;
    /** Set when this post is returned from the ML recommendation feed. */
    private Boolean isRecommended;
    /** Deterministic score used when feed is sorted by popularity. */
    private Double engagementScore;
    private List<CommentResponse> comments;
    private List<MediaAttachmentDto> attachments;
}
