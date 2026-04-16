package com.civicplatform.repository;

import com.civicplatform.entity.Post;
import com.civicplatform.enums.PostStatus;
import com.civicplatform.enums.PostType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    
    List<Post> findByStatus(PostStatus status);
    
    Page<Post> findByStatus(PostStatus status, Pageable pageable);
    
    List<Post> findByType(PostType type);
    
    Page<Post> findByType(PostType type, Pageable pageable);
    
    List<Post> findByCampaignId(Long campaignId);
    
    @Query("SELECT p FROM Post p WHERE p.creator = :creator ORDER BY p.createdAt DESC")
    List<Post> findByCreator(String creator);
    
    @Query("SELECT COUNT(p) FROM Post p WHERE p.status = :status")
    long countByStatus(PostStatus status);

    long countByCreatorAndType(String creator, PostType type);
}
