package com.civicplatform.controller;

import com.civicplatform.dto.request.PostRequest;
import com.civicplatform.dto.response.PostResponse;
import com.civicplatform.entity.PostAttachment;
import com.civicplatform.entity.User;
import com.civicplatform.enums.PostStatus;
import com.civicplatform.enums.PostType;
import com.civicplatform.repository.PostAttachmentRepository;
import com.civicplatform.security.CurrentUserResolver;
import com.civicplatform.security.RegularAccountPolicy;
import com.civicplatform.service.PostMediaStorageService;
import com.civicplatform.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping({"/posts", "/v1/posts"})
@RequiredArgsConstructor
@Tag(name = "Post Management", description = "Post management APIs")
public class PostController {

    private final PostService postService;
    private final CurrentUserResolver currentUserResolver;
    private final PostAttachmentRepository postAttachmentRepository;
    private final PostMediaStorageService postMediaStorageService;

    @Operation(summary = "Create a new post (JSON body)")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PostResponse> createPost(@Valid @RequestBody PostRequest postRequest, Authentication authentication) {
        User user = getUserFromAuthentication(authentication);
        RegularAccountPolicy.requireRegularUser(user);
        Long userId = user.getId();
        PostResponse response = postService.createPost(postRequest, userId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Create a new post with optional image/video attachments")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PostResponse> createPostMultipart(
            @RequestParam(required = false) String content,
            @RequestParam String type,
            @RequestParam(required = false) Long campaignId,
            @RequestParam(value = "files", required = false) MultipartFile[] files,
            Authentication authentication) {
        User user = getUserFromAuthentication(authentication);
        RegularAccountPolicy.requireRegularUser(user);
        PostType postType = PostType.valueOf(type.trim());
        List<MultipartFile> list = files == null ? List.of() : Arrays.asList(files);
        PostResponse response = postService.createPostWithMedia(content, postType, campaignId, list, user.getId());
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Operation(summary = "Download a post media attachment (image or video)")
    @GetMapping("/{postId}/attachments/{attachmentId}")
    public ResponseEntity<Resource> getPostAttachment(
            @PathVariable Long postId,
            @PathVariable Long attachmentId) {
        PostAttachment a = postAttachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Attachment not found"));
        if (!a.getPost().getId().equals(postId)) {
            return ResponseEntity.notFound().build();
        }
        try {
            Resource resource = postMediaStorageService.loadPostResource(postId, a.getFilename());
            if (resource == null || !resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }
            MediaType mt = a.getMimeType() != null
                    ? MediaType.parseMediaType(a.getMimeType())
                    : MediaType.APPLICATION_OCTET_STREAM;
            return ResponseEntity.ok().contentType(mt).body(resource);
        } catch (IOException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Get all posts")
    @GetMapping
    public ResponseEntity<List<PostResponse>> getAllPosts() {
        List<PostResponse> response = postService.getAllPosts();
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get post feed sorted by deterministic score")
    @GetMapping("/feed")
    public ResponseEntity<List<PostResponse>> getFeed(
            @RequestParam(name = "sort", defaultValue = "recent") String sort) {
        if ("popularity".equalsIgnoreCase(sort)) {
            return ResponseEntity.ok(postService.getFeedByPopularity());
        }
        return ResponseEntity.ok(postService.getAllPosts());
    }

    @Operation(summary = "Get current user's posts")
    @GetMapping("/my")
    public ResponseEntity<List<PostResponse>> getMyPosts(Authentication authentication) {
        User user = getUserFromAuthentication(authentication);
        RegularAccountPolicy.requireRegularUser(user);
        List<PostResponse> response = postService.getPostsByCreator(user.getUserName());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get post by ID")
    @GetMapping("/{id}")
    public ResponseEntity<PostResponse> getPostById(@PathVariable Long id) {
        PostResponse response = postService.getPostById(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get posts by status")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<PostResponse>> getPostsByStatus(@PathVariable PostStatus status) {
        List<PostResponse> response = postService.getPostsByStatus(status);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get posts by campaign")
    @GetMapping("/campaign/{campaignId}")
    public ResponseEntity<List<PostResponse>> getPostsByCampaign(@PathVariable Long campaignId) {
        List<PostResponse> response = postService.getPostsByCampaign(campaignId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update post")
    @PutMapping("/{id}")
    public ResponseEntity<PostResponse> updatePost(@PathVariable Long id, @Valid @RequestBody PostRequest postRequest, Authentication authentication) {
        checkPostOwnership(id, authentication);
        PostResponse response = postService.updatePost(id, postRequest);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete post")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id, Authentication authentication) {
        checkPostOwnership(id, authentication);
        postService.deletePost(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Approve post")
    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PostResponse> approvePost(@PathVariable Long id) {
        PostResponse response = postService.approvePost(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Reject post")
    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PostResponse> rejectPost(@PathVariable Long id) {
        PostResponse response = postService.rejectPost(id);
        return ResponseEntity.ok(response);
    }

    private void checkPostOwnership(Long postId, Authentication authentication) {
        User user = currentUserResolver.resolveRequired(authentication);
        RegularAccountPolicy.requireRegularUser(user);
        PostResponse post = postService.getPostById(postId);
        if (!user.getUserName().equals(post.getCreator())) {
            throw new org.springframework.security.access.AccessDeniedException("You are not the owner of this post");
        }
    }

    private User getUserFromAuthentication(Authentication authentication) {
        return currentUserResolver.resolveRequired(authentication);
    }
}
