package jamm.board.article.service.request;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class ArticleCreateRequest {

    private String title;
    private String content;
    private Long writeId;
    private Long boardId;
}
