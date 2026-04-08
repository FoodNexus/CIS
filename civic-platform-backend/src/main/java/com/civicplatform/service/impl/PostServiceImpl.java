package com.civicplatform.service.impl;

import com.civicplatform.dto.request.PostRequest;
import com.civicplatform.dto.response.PostResponse;
import com.civicplatform.entity.Campaign;
import com.civicplatform.entity.Post;
import com.civicplatform.entity.User;
import com.civicplatform.enums.PostStatus;
import com.civicplatform.mapper.PostMapper;
import com.civicplatform.repository.CampaignRepository;
import com.civicplatform.repository.PostRepository;
import com.civicplatform.repository.UserRepository;
import com.civicplatform.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CampaignRepository campaignRepository;
    private final PostMapper postMapper;

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
        return postMapper.toResponse(post);
    }

    @Override
    public PostResponse getPostById(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + id));
        return postMapper.toResponse(post);
    }

    @Override
    public List<PostResponse> getAllPosts() {
        List<Post> posts = postRepository.findAll();
        return posts.stream().map(postMapper::toSummaryResponse).collect(Collectors.toList());
    }

    @Override
    public List<PostResponse> getPostsByCreator(String creator) {
        List<Post> posts = postRepository.findByCreator(creator);
        return posts.stream().map(postMapper::toSummaryResponse).collect(Collectors.toList());
    }

    @Override
    public List<PostResponse> getPostsByStatus(PostStatus status) {
        List<Post> posts = postRepository.findByStatus(status);
        return posts.stream().map(postMapper::toSummaryResponse).collect(Collectors.toList());
    }

    @Override
    public List<PostResponse> getPostsByCampaign(Long campaignId) {
        List<Post> posts = postRepository.findByCampaignId(campaignId);
        return posts.stream().map(postMapper::toSummaryResponse).collect(Collectors.toList());
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
        return postMapper.toResponse(post);
    }

    @Override
    @Transactional(readOnly = false)
    public void deletePost(Long id) {
        if (!postRepository.existsById(id)) {
            throw new RuntimeException("Post not found with id: " + id);
        }
        postRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = false)
    public PostResponse approvePost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + id));
        
        post.setStatus(PostStatus.ACCEPTED);
        post = postRepository.save(post);
        return postMapper.toResponse(post);
    }

    @Override
    @Transactional(readOnly = false)
    public PostResponse rejectPost(Long id) {
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + id));
        
        post.setStatus(PostStatus.REJECTED);
        post = postRepository.save(post);
        return postMapper.toResponse(post);
    }
}
