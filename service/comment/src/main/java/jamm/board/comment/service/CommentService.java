package jamm.board.comment.service;

import jamm.board.comment.entity.Comment;
import jamm.board.comment.repository.CommentRepository;
import jamm.board.comment.service.request.CommentCreateRequest;
import jamm.board.comment.service.response.CommentPageResponse;
import jamm.board.comment.service.response.CommentResponse;
import kuke.board.common.snowflake.Snowflake;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static java.util.function.Predicate.not;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final Snowflake snowflake = new Snowflake();
    private final CommentRepository commentRepository;

    /**
     * 댓글 생성
     */
    @Transactional
    public CommentResponse create(CommentCreateRequest request) {

        //parent로 가져온 값이 존재하다면, 그 부모 댓글(parent)와 새로 생성할 댓글을 연결해줌
        Comment parent = findParent(request);

        Comment comment = commentRepository.save(
                Comment.create(
                        snowflake.nextId(),
                        request.getContent(),
                        parent == null ? null : parent.getParentCommentId(),
                        request.getArticleId(),
                        request.getWriterId()
                )
        );
        return CommentResponse.from(comment);

    }

    //부모 댓글 반환
    private Comment findParent(CommentCreateRequest request) {
        Long parentCommentId = request.getParentCommentId();
        if (parentCommentId == null) {
            return null;
        }
        return commentRepository.findById(parentCommentId)
                .filter(not(Comment::getDeleted)) // 삭제되지 않은 댓글이여야 함.
                .filter(Comment::isRoot) // 루트 댓글(최상위 댓글)이여야 함.
                .orElseThrow();
    }

    /**
     * 댓글 조회
     */
    public CommentResponse read(Long commentId) {
        return CommentResponse.from(
                commentRepository.findById(commentId).orElseThrow()
        );
    }

    /**
     * 댓글 삭제
     */
    @Transactional
    public void delete(Long commentId) {
        commentRepository.findById(commentId)
                .filter(not(Comment::getDeleted)) // 삭제되지 않은 댓글이여야 함.
                .ifPresent(comment -> { //존재하고
                    if(hasChildren(comment)) { //만약 자식 댓글이 있다면
                        comment.delete(); //boolean값 deleted를 true로 바꿔줌 (soft delete)
                    } else {
                        delete(comment); //실제 삭제
                    }
                });
    }

    private boolean hasChildren(Comment comment) {
        //getCommentId를 넘겨야 해당 comment의 하위 댓글이 있는지 확인할 수 있다!
        //2인 이유 자신(comment) + 자식 1개
        return commentRepository.countBy(comment.getArticleId(), comment.getCommentId(), 2L) == 2;
    }

    /**
     * 댓글 삭제
     */
    private void delete(Comment comment) {
        commentRepository.delete(comment); //부모댓글을 먼저 제거하고
        if(!comment.isRoot()) { //대댓글일 경우
            commentRepository.findById(comment.getParentCommentId()) //부모 댓글 조회
                    .filter(Comment::getDeleted) //부모 댓글은 이미 삭제된 상태여야 함.
                    .filter(not(this::hasChildren)) //부모 댓글은 자식 댓글을 가지고 있지 않아야 함
                    .ifPresent(this::delete); //부모 댓글이 존재하다면 부모댓글도 삭제
        }
    }

    /**
     * 댓글 전체 조회 (2 depth 페이지 번호)
     */
    public CommentPageResponse readAll(Long articleId, Long page, Long pageSize) {
        return CommentPageResponse.of(
                commentRepository.findAll(articleId, (page - 1) * pageSize, pageSize).stream()
                        .map(CommentResponse::from)
                        .toList(),
                commentRepository.count(articleId, PageLimitCalculator.calculatePageLimit(page, pageSize, 10L)) //한 페이지당 10개 댓글 가정
        );
    }

    /**
     * 댓글 전체 조회 (2 depth 무한 스크롤)
     */
    public List<CommentResponse> readAll(Long articleId, Long lastParentCommentId, Long lastCommentId, Long limit) {
        List<Comment> comments = lastParentCommentId == null || lastCommentId == null ?
                commentRepository.findAllInfiniteScroll(articleId, limit) :
                commentRepository.findAllInfiniteScroll(articleId, lastParentCommentId, lastCommentId, limit);
        return comments.stream()
                .map(CommentResponse::from)
                .toList();
    }


}
