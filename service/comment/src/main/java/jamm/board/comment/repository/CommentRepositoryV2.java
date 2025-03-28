package jamm.board.comment.repository;

import jamm.board.comment.entity.CommentV2;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommentRepositoryV2 extends JpaRepository<CommentV2,Long> {

    @Query("select c from CommentV2 c where c.commentPath = :path")
    Optional<CommentV2> findByPath(@Param("path") String path);

}
