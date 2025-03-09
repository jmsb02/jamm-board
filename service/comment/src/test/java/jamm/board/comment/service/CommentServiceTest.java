package jamm.board.comment.service;

import jamm.board.comment.entity.Comment;
import jamm.board.comment.repository.CommentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @InjectMocks
    CommentService commentService;

    @Mock
    CommentRepository commentRepository;

    @Test
    @DisplayName("삭제할 댓글이 자식이 있으면 삭제 표시만 한다.")
    void deleteShouldMarkDeletedIfHasChildren() {
        //given
        Long articleId = 1L;
        Long commentId = 2L;
        Comment comment = createComment(articleId, commentId);
        given(commentRepository.findById(commentId))
                .willReturn(Optional.of(comment));
        //자식댓글이 가능하도록 설정
        given(commentRepository.countBy(articleId, commentId,2L)).willReturn(2L);

        //when
        commentService.delete(commentId);

        //then (soft delete)
        verify(comment).delete();
    }

    @Test
    @DisplayName("하위 댓글이 삭제되고, 삭제되지 않은 부모면, 하위 댓글만 삭제한다.")
    void deleteShouldDeleteChildOnlyIfNotDeletedParent() {
        //given
        Long articleId = 1L;
        Long commentId = 2L;
        Long parentCommentId = 1L;

        //자식 댓글 Mock 생성
        Comment comment = createComment(articleId, commentId, parentCommentId);
        given(comment.isRoot()).willReturn(false); //대댓글 명시

        //부모 댓글 Mock 생성 (삭제되지 않은 상태)
        Comment parentComment = mock(Comment.class);
        given(parentComment.getDeleted()).willReturn(false);

        given(commentRepository.findById(commentId))
                .willReturn(Optional.of(comment)); //자식 댓글 조회

        given(commentRepository.countBy(articleId, commentId,2L)).willReturn(1L); //자식 댓글 1개 존재

        given(commentRepository.findById(parentCommentId))
                .willReturn(Optional.of(parentComment)); //부모 댓글 조회

        //when
        commentService.delete(commentId);

        //then (soft delete)
        verify(commentRepository).delete(comment); //대댓글만 삭제
        verify(commentRepository, never()).delete(parentComment); //부모 댓글은 삭제 X (자식 댓글이 존재하기 때문에)
    }

    @Test
    @DisplayName("하위 댓글이 삭제되고, 삭제된 부모면, 재귀적으로 모두 삭제한다.")
    void deleteShouldDeleteAllRecursivelyIfDeleteParent() {
        //given
        Long articleId = 1L;
        Long commentId = 2L;
        Long parentCommentId = 1L;

        //대댓글 생성
        Comment comment = createComment(articleId, commentId, parentCommentId);
        given(comment.isRoot()).willReturn(false);

        //부모 댓글 생성 (이미 삭제된 상태)
        Comment parentComment = createComment(articleId, parentCommentId);
        given(parentComment.isRoot()).willReturn(true);
        given(parentComment.getDeleted()).willReturn(true);

        given(commentRepository.findById(commentId))
                .willReturn(Optional.of(comment));

        //대댓글의 자식 댓글이 1개 존재
        given(commentRepository.countBy(articleId, commentId,2L)).willReturn(1L);

        //부모 댓글 조회
        given(commentRepository.findById(parentCommentId))
                .willReturn(Optional.of(parentComment));

        // 부모 댓글의 자식 댓글도 1개 (재귀 삭제 조건 충족)
        given(commentRepository.countBy(articleId, parentCommentId,2L)).willReturn(1L);


        //when
        commentService.delete(commentId);

        //then
        verify(commentRepository).delete(comment); //대댓글 삭제
        verify(commentRepository).delete(parentComment); //부모 댓글도 삭제
    }

    private Comment createComment(Long articleId, Long commentId) {
        Comment comment = mock(Comment.class);
        given(comment.getArticleId()).willReturn(articleId);
        given(comment.getCommentId()).willReturn(commentId);
        return comment;
    }

    private Comment createComment(Long articleId, Long commentId, Long parentCommentId) {
        Comment comment = createComment(articleId, commentId);
        given(comment.getParentCommentId()).willReturn(parentCommentId);
        return comment;
    }


}