package com.civicplatform.service;

import com.civicplatform.dto.request.PostRequest;
import com.civicplatform.dto.response.PostResponse;
import com.civicplatform.enums.PostStatus;
import com.civicplatform.enums.PostType;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PostService {
    PostResponse createPost(PostRequest postRequest, Long authorId);
    PostResponse createPostWithMedia(String content, PostType type, Long campaignId, List<MultipartFile> files, Long authorId);
    PostResponse getPostById(Long id);
    List<PostResponse> getAllPosts();
    List<PostResponse> getFeedByPopularity();

    List<PostResponse> getPostsByCreator(String creator);
    List<PostResponse> getPostsByStatus(PostStatus status);
    List<PostResponse> getPostsByCampaign(Long campaignId);
    PostResponse updatePost(Long id, PostRequest postRequest);
    void deletePost(Long id);
    PostResponse approvePost(Long id);
    PostResponse rejectPost(Long id);
}
