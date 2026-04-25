package com.civicplatform.service.impl;

import com.civicplatform.dto.request.PostRequest;
import com.civicplatform.dto.response.MediaAttachmentDto;
import com.civicplatform.dto.response.PostResponse;
import com.civicplatform.entity.Campaign;
import com.civicplatform.entity.Post;
import com.civicplatform.entity.PostAttachment;
import com.civicplatform.entity.User;
import com.civicplatform.enums.NotificationType;
import com.civicplatform.enums.PostStatus;
import com.civicplatform.enums.PostType;
import com.civicplatform.mapper.PostMapper;
import com.civicplatform.repository.CampaignRepository;
import com.civicplatform.repository.CommentRepository;
import com.civicplatform.repository.PostAttachmentRepository;
import com.civicplatform.repository.PostRepository;
import com.civicplatform.repository.UserRepository;
import com.civicplatform.service.NotificationService;
import com.civicplatform.service.PostMediaStorageService;
import com.civicplatform.service.PostService;
import com.civicplatform.service.ScoringProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostServiceImpl implements PostService {

    private static final int MAX_MEDIA_FILES = 10;

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CampaignRepository campaignRepository;
    private final CommentRepository commentRepository;
    private final PostMapper postMapper;
    private final PostAttachmentRepository postAttachmentRepository;
    private final PostMediaStorageService postMediaStorageService;
    private final NotificationService notificationService;
    private final ScoringProperties scoringProperties;

    @Override
    @Transactional(readOnly = false)
    public PostResponse createPost(PostRequest postRequest, Long authorId) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + authorId));

        Post post = postMapper.toEntity(postRequest);
        post.setCreator(author.getUserName());

        if (postRequest.getCampaignId() != null) {
            Campaign campaign = campaignRepository.findById(postRequest.getCampaignId())
                    .orElseThrow(() -> new RuntimeException("Campaign not found with id: " + postRequest.getCampaignId()));
            post.setCampaign(campaign);
        }

        post = postRepository.save(post);
        PostResponse r = postMapper.toResponse(post);
        r.setAttachments(List.of());
        return r;
    }

    @Override
    @Transactional(readOnly = false)
    public PostResponse createPostWithMedia(String content, PostType type, Long campaignId, List<MultipartFile> files, Long authorId) {
        List<MultipartFile> fileList = files == null ? List.of() : files;
        if (fileList.size() > MAX_MEDIA_FILES) {
            throw new IllegalArgumentException("At most " + MAX_MEDIA_FILES + " media files per post.");
        }
        String text = content == null ? null : content.trim();
        if ((text == null || text.isEmpty()) && fileList.stream().allMatch(f -> f == null || f.isEmpty())) {
            throw new IllegalArgumentException("Post must have text or at least one media file.");
        }

        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + authorId));

        Post post = Post.builder()
                .creator(author.getUserName())
                .content(emptyToNull(text))
                .type(type)
                .status(PostStatus.PENDING)
                .likesCount(0)
                .build();
        if (campaignId != null) {
            Campaign campaign = campaignRepository.findById(campaignId)
                    .orElseThrow(() -> new RuntimeException("Campaign not found with id: " + campaignId));
            post.setCampaign(campaign);
        }
        post = postRepository.save(post);

        for (MultipartFile file : fileList) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            try {
                PostAttachment att = postMediaStorageService.storePostFile(post, file);
                postAttachmentRepository.save(att);
            } catch (IOException e) {
                throw new RuntimeException("Failed to store media attachment.", e);
            }
        }

        Post refreshed = postRepository.findById(post.getId()).orElseThrow();
        PostResponse r = postMapper.toSummaryResponse(refreshed);
        r.setAttachments(toPostAttachmentDtos(refreshed.getId(),
                postAttachmentRepository.findByPost_IdOrderByIdAsc(refreshed.getId())));
        return r;
    }

    private static String emptyToNull(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        return s;
    }

    @Override
    public PostResponse getPostById(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + id));
        PostResponse r = postMapper.toResponse(post);
        r.setAttachments(toPostAttachmentDtos(post.getId(),
                postAttachmentRepository.findByPost_IdOrderByIdAsc(post.getId())));
        return r;
    }

    @Override
    public List<PostResponse> getAllPosts() {
        List<Post> posts = postRepository.findAll();
        return mapPostsWithAttachments(posts);
    }

    /**
     * Returns posts sorted by deterministic engagement popularity score.
     */
    @Override
    public List<PostResponse> getFeedByPopularity() {
        ScoringProperties.PostEngagement cfg = scoringProperties.getPostEngagement();
        List<Post> posts = postRepository.findAll();
        record PostWithScore(Post post, double score) {}
        List<PostWithScore> scored = posts.stream()
                .map(post -> new PostWithScore(post, calculateEngagementScore(post, cfg)))
                .toList();

        return scored.stream()
                .sorted((left, right) -> {
                    int byScore = Double.compare(right.score(), left.score());
                    if (byScore != 0) {
                        return byScore;
                    }
                    Post rightPost = right.post();
                    Post leftPost = left.post();
                    LocalDateTime rightCreated = rightPost.getCreatedAt() == null ? LocalDateTime.MIN : rightPost.getCreatedAt();
                    LocalDateTime leftCreated = leftPost.getCreatedAt() == null ? LocalDateTime.MIN : leftPost.getCreatedAt();
                    int byCreatedAt = rightCreated.compareTo(leftCreated);
                    if (byCreatedAt != 0) {
                        return byCreatedAt;
                    }
                    Long rightId = rightPost.getId() == null ? Long.MIN_VALUE : rightPost.getId();
                    Long leftId = leftPost.getId() == null ? Long.MIN_VALUE : leftPost.getId();
                    return rightId.compareTo(leftId);
                })
                .map(item -> {
                    Post p = item.post();
                    PostResponse r = postMapper.toSummaryResponse(p);
                    r.setEngagementScore(item.score());
                    r.setAttachments(toPostAttachmentDtos(p.getId(),
                            postAttachmentRepository.findByPost_IdOrderByIdAsc(p.getId())));
                    return r;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<PostResponse> getPostsByCreator(String creator) {
        List<Post> posts = postRepository.findByCreator(creator);
        return mapPostsWithAttachments(posts);
    }

    @Override
    public List<PostResponse> getPostsByStatus(PostStatus status) {
        List<Post> posts = postRepository.findByStatus(status);
        return mapPostsWithAttachments(posts);
    }

    @Override
    public List<PostResponse> getPostsByCampaign(Long campaignId) {
        List<Post> posts = postRepository.findByCampaignId(campaignId);
        return mapPostsWithAttachments(posts);
    }

    private List<PostResponse> mapPostsWithAttachments(List<Post> posts) {
        List<Long> ids = posts.stream().map(Post::getId).toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        List<PostAttachment> all = postAttachmentRepository.findByPost_IdIn(ids);
        Map<Long, List<PostAttachment>> byPost = all.stream()
                .collect(Collectors.groupingBy(a -> a.getPost().getId()));
        return posts.stream().map(p -> {
            PostResponse r = postMapper.toSummaryResponse(p);
            List<PostAttachment> atts = byPost.getOrDefault(p.getId(), List.of()).stream()
                    .sorted(Comparator.comparing(PostAttachment::getId))
                    .toList();
            r.setAttachments(toPostAttachmentDtos(p.getId(), atts));
            return r;
        }).collect(Collectors.toList());
    }

    private List<MediaAttachmentDto> toPostAttachmentDtos(Long postId, List<PostAttachment> list) {
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        return list.stream().map(a -> MediaAttachmentDto.builder()
                .id(a.getId())
                .kind(a.getKind().name())
                .url("/posts/" + postId + "/attachments/" + a.getId())
                .build()).collect(Collectors.toList());
    }

    private double calculateEngagementScore(Post post, ScoringProperties.PostEngagement cfg) {
        long likes = post.getLikesCount() == null ? 0L : post.getLikesCount();
        long comments = post.getId() == null ? 0L : commentRepository.countByPostId(post.getId());
        long shares = 0L; // No share entity yet; keep deterministic and explicit.

        long hoursSincePost = 0L;
        if (post.getCreatedAt() != null) {
            hoursSincePost = Math.max(0L, Duration.between(post.getCreatedAt(), LocalDateTime.now()).toHours());
        }
        double decay = 1.0d / (hoursSincePost + 1.0d);
        double score = ((likes * cfg.getWeightLikes())
                + (comments * cfg.getWeightComments())
                + (shares * cfg.getWeightShares())) * decay;
        log.debug("post={} likes={} comments={} shares={} hours={} score={}",
                post.getId(), likes, comments, shares, hoursSincePost, score);
        return score;
    }

    @Override
    @Transactional(readOnly = false)
    public PostResponse updatePost(Long id, PostRequest postRequest) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + id));

        postMapper.updateEntity(postRequest, post);

        if (postRequest.getCampaignId() != null) {
            Campaign campaign = campaignRepository.findById(postRequest.getCampaignId())
                    .orElseThrow(() -> new RuntimeException("Campaign not found with id: " + postRequest.getCampaignId()));
            post.setCampaign(campaign);
        }

        post = postRepository.save(post);
        PostResponse r = postMapper.toResponse(post);
        r.setAttachments(toPostAttachmentDtos(post.getId(),
                postAttachmentRepository.findByPost_IdOrderByIdAsc(post.getId())));
        return r;
    }

    @Override
    @Transactional(readOnly = false)
    public void deletePost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + id));
        postMediaStorageService.deletePostFolder(id);
        postRepository.delete(post);
    }

    @Override
    @Transactional(readOnly = false)
    public PostResponse approvePost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + id));

        post.setStatus(PostStatus.ACCEPTED);
        post = postRepository.save(post);
        notificationService.notifyByUserNameUnless(
                post.getCreator(),
                null,
                NotificationType.MODERATION,
                "Post approved",
                "Your post was accepted and is visible in the feed.",
                "/posts/" + post.getId());
        PostResponse r = postMapper.toResponse(post);
        r.setAttachments(toPostAttachmentDtos(post.getId(),
                postAttachmentRepository.findByPost_IdOrderByIdAsc(post.getId())));
        return r;
    }

    @Override
    @Transactional(readOnly = false)
    public PostResponse rejectPost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + id));

        post.setStatus(PostStatus.REJECTED);
        post = postRepository.save(post);
        notificationService.notifyByUserNameUnless(
                post.getCreator(),
                null,
                NotificationType.WARNING,
                "Post not approved",
                "Your post was not approved. You can edit and resubmit from My posts.",
                "/my-posts");
        PostResponse r = postMapper.toResponse(post);
        r.setAttachments(toPostAttachmentDtos(post.getId(),
                postAttachmentRepository.findByPost_IdOrderByIdAsc(post.getId())));
        return r;
    }
}
