package jamm.board.comment.repository;

import jamm.board.comment.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * 특정 게시글에서 해당 parentCommentId가 id인 갯수
     */
    @Query(
            value = "select count(*) from (" +
                    "select comment_id from comment " +
                    "where article_id = :articleId and parent_comment_id = :parentCommentId" +
                    "limit :limit" +
                    ")",
            nativeQuery = true
    )
    Long countBy(
            @Param("articleId") Long articleId,
            @Param("parentCommentId") Long parentCommentId,
            @Param("limit") Long limit
    );
}
