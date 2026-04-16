package com.civicplatform.repository;

import com.civicplatform.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    
    List<Comment> findByPostIdOrderByCreatedAtDesc(Long postId);
    
    List<Comment> findByAuthorId(Long authorId);
    
    void deleteByPostId(Long postId);

    @Query("SELECT COUNT(c) FROM Comment c JOIN c.post p WHERE c.author.id = :userId AND ("
            + "LOWER(COALESCE(p.content,'')) LIKE '%recipe%' OR LOWER(COALESCE(p.content,'')) LIKE '%cooking%' OR "
            + "LOWER(COALESCE(p.content,'')) LIKE '%food%' OR LOWER(COALESCE(p.content,'')) LIKE '%leftover%')")
    long countFoodRelatedCommentsByAuthor(@Param("userId") Long userId);
}
