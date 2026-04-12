package com.civicplatform.service.impl;

import com.civicplatform.dto.request.CommentRequest;
import com.civicplatform.dto.response.CommentResponse;
import com.civicplatform.dto.response.MediaAttachmentDto;
import com.civicplatform.entity.Comment;
import com.civicplatform.entity.CommentAttachment;
import com.civicplatform.entity.Post;
import com.civicplatform.entity.User;
import com.civicplatform.mapper.CommentMapper;
import com.civicplatform.repository.CommentAttachmentRepository;
import com.civicplatform.repository.CommentRepository;
import com.civicplatform.repository.PostRepository;
import com.civicplatform.repository.UserRepository;
import com.civicplatform.enums.InteractionAction;
import com.civicplatform.enums.InteractionEntityType;
import com.civicplatform.service.CommentService;
import com.civicplatform.service.UserInteractionService;
import com.civicplatform.service.PostMediaStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private static final int MAX_MEDIA_FILES = 10;

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentMapper commentMapper;
    private final CommentAttachmentRepository commentAttachmentRepository;
    private final PostMediaStorageService postMediaStorageService;
    private final UserInteractionService userInteractionService;

    @Override
    @Transactional
    public CommentResponse createComment(CommentRequest commentRequest, Long authorId) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + authorId));

        Post post = postRepository.findById(commentRequest.getPostId())
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + commentRequest.getPostId()));

        Comment comment = commentMapper.toEntity(commentRequest);
        comment.setAuthor(author);
        comment.setPost(post);

        comment = commentRepository.save(comment);
        userInteractionService.record(authorId, InteractionEntityType.POST, post.getId(), InteractionAction.COMMENT);
        CommentResponse r = commentMapper.toResponse(comment);
        r.setAttachments(List.of());
        return r;
    }

    @Override
    @Transactional
    public CommentResponse createCommentWithMedia(String content, Long postId, List<MultipartFile> files, Long authorId) {
        List<MultipartFile> fileList = files == null ? List.of() : files;
        if (fileList.size() > MAX_MEDIA_FILES) {
            throw new IllegalArgumentException("At most " + MAX_MEDIA_FILES + " media files per comment.");
        }
        String text = content == null ? null : content.trim();
        if ((text == null || text.isEmpty()) && fileList.stream().allMatch(f -> f == null || f.isEmpty())) {
            throw new IllegalArgumentException("Comment must have text or at least one media file.");
        }

        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + authorId));
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + postId));

        Comment comment = Comment.builder()
                .content(emptyToNull(text))
                .author(author)
                .post(post)
                .build();
        comment = commentRepository.save(comment);
        userInteractionService.record(authorId, InteractionEntityType.POST, postId, InteractionAction.COMMENT);

        for (MultipartFile file : fileList) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            try {
                CommentAttachment att = postMediaStorageService.storeCommentFile(comment, file);
                commentAttachmentRepository.save(att);
            } catch (IOException e) {
                throw new RuntimeException("Failed to store media attachment.", e);
            }
        }

        Comment refreshed = commentRepository.findById(comment.getId()).orElseThrow();
        CommentResponse r = commentMapper.toResponse(refreshed);
        r.setAttachments(toCommentAttachmentDtos(refreshed.getId(),
                commentAttachmentRepository.findByComment_IdOrderByIdAsc(refreshed.getId())));
        return r;
    }

    private static String emptyToNull(String s) {
        if (s == null || s.isEmpty()) {
            return null;
        }
        return s;
    }

    @Override
    public CommentResponse getCommentById(Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found with id: " + id));
        CommentResponse r = commentMapper.toResponse(comment);
        r.setAttachments(toCommentAttachmentDtos(comment.getId(),
                commentAttachmentRepository.findByComment_IdOrderByIdAsc(comment.getId())));
        return r;
    }

    @Override
    public List<CommentResponse> getCommentsByPost(Long postId) {
        List<Comment> comments = commentRepository.findByPostIdOrderByCreatedAtDesc(postId);
        List<Long> ids = comments.stream().map(Comment::getId).toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        List<CommentAttachment> all = commentAttachmentRepository.findByComment_IdIn(ids);
        Map<Long, List<CommentAttachment>> byComment = all.stream()
                .collect(Collectors.groupingBy(a -> a.getComment().getId()));
        return comments.stream().map(c -> {
            CommentResponse r = commentMapper.toResponse(c);
            List<CommentAttachment> atts = byComment.getOrDefault(c.getId(), List.of()).stream()
                    .sorted(Comparator.comparing(CommentAttachment::getId))
                    .toList();
            r.setAttachments(toCommentAttachmentDtos(c.getId(), atts));
            return r;
        }).collect(Collectors.toList());
    }

    @Override
    public List<CommentResponse> getCommentsByAuthor(Long authorId) {
        List<Comment> comments = commentRepository.findByAuthorId(authorId);
        List<Long> ids = comments.stream().map(Comment::getId).toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        List<CommentAttachment> all = commentAttachmentRepository.findByComment_IdIn(ids);
        Map<Long, List<CommentAttachment>> byComment = all.stream()
                .collect(Collectors.groupingBy(a -> a.getComment().getId()));
        return comments.stream().map(c -> {
            CommentResponse r = commentMapper.toResponse(c);
            List<CommentAttachment> atts = byComment.getOrDefault(c.getId(), List.of()).stream()
                    .sorted(Comparator.comparing(CommentAttachment::getId))
                    .toList();
            r.setAttachments(toCommentAttachmentDtos(c.getId(), atts));
            return r;
        }).collect(Collectors.toList());
    }

    private List<MediaAttachmentDto> toCommentAttachmentDtos(Long commentId, List<CommentAttachment> list) {
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        return list.stream().map(a -> MediaAttachmentDto.builder()
                .id(a.getId())
                .kind(a.getKind().name())
                .url("/comments/" + commentId + "/attachments/" + a.getId())
                .build()).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CommentResponse updateComment(Long id, CommentRequest commentRequest) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found with id: " + id));

        commentMapper.updateEntity(commentRequest, comment);
        comment = commentRepository.save(comment);
        CommentResponse r = commentMapper.toResponse(comment);
        r.setAttachments(toCommentAttachmentDtos(comment.getId(),
                commentAttachmentRepository.findByComment_IdOrderByIdAsc(comment.getId())));
        return r;
    }

    @Override
    @Transactional
    public void deleteComment(Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found with id: " + id));
        postMediaStorageService.deleteCommentFolder(id);
        commentRepository.delete(comment);
    }
}
