package jamm.board.comment.entity;

import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommentPath {
    private String path;

    private static final String CHARSET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private static final int DEPTH_CHUNK_SIZE = 5;
    private static final int MAX_DEPTH = 5;

    // MIN_CHUNK = "00000" MAX_CHUNK = "zzzzz"
    private static final String MIN_CHUNK = String.valueOf(CHARSET.charAt(0)).repeat(DEPTH_CHUNK_SIZE);
    private static final String MAX_CHUNK = String.valueOf(CHARSET.charAt(CHARSET.length() - 1)).repeat(DEPTH_CHUNK_SIZE);

    /**
     * 팩토리 메서드
     */
    public static CommentPath create(String path) {
        if(isDepthOverflowed(path)) { // depth > 5 일경우 오류 반환
            throw new IllegalStateException("depth overflowed");
        }
        CommentPath commentPath = new CommentPath();
        commentPath.path = path;
        return commentPath;
    }

    private static boolean isDepthOverflowed(String path) {
        return calDepth(path) > MAX_DEPTH;
    }

    /**
     * Depth 계산 메서드 - 받아온 path의 길이 / 5
     */
    private static int calDepth(String path) {
        return path.length() / DEPTH_CHUNK_SIZE;
    }

    public int getDepth() {
        return calDepth(path);
    }

    public boolean isRoot() {
        return calDepth(path) == 1;
    }

    public String getParentPath() {
        // 00000 0000z  -> 00000 반환
        return path.substring(0, path.length() - DEPTH_CHUNK_SIZE);
    }

    /**
     * 신규 댓글의 path를 구하기 위한 메서드 : childrenTopPath를 구한 뒤 1을 더해준다.
     */
    public CommentPath createChildCommentPath(String descendantsTopPath) {
        if (descendantsTopPath == null) {
            return CommentPath.create(path + MIN_CHUNK); //MIN_CHUNK = 00000
        }
        String childrenTopPath = findChildrenTopPath(descendantsTopPath); // descendantsTopPath를 통해 childrenTopPath를 구해준다.
        return CommentPath.create(increase(childrenTopPath)); // 1을 더해준다.
    }

    private String findChildrenTopPath(String descendantsTopPath) {
        //descendantsTopPath에 childrenTopPath가 포함되어 짤라준다.
        return descendantsTopPath.substring(0, (getDepth() + 1) * DEPTH_CHUNK_SIZE);
    }

    private String increase(String path) {
        //00000 00000 여기서 마지막 00000 짤라줌
        String lastChunk = path.substring(path.length() - DEPTH_CHUNK_SIZE);
        if (isChunkOverflowed(lastChunk)) { // 이 때 childrenTopPath = zzzzz까지 댓글이 생성되어 있는 경우 방지
            throw new IllegalStateException("chunk overflowed");
        }

        int charsetLength = CHARSET.length();

        int value = 0;
        for (char ch : lastChunk.toCharArray()) {
            value = value * charsetLength + CHARSET.indexOf(ch);
        }

        value = value + 1;

        String result = "";

        for(int i=0; i<DEPTH_CHUNK_SIZE;i++) {
            result = CHARSET.charAt(value % charsetLength) + result;
            value /= charsetLength;
        }

        return path.substring(0, path.length() - DEPTH_CHUNK_SIZE) + result;

    }

    private boolean isChunkOverflowed(String lastChunk) {
        return MAX_CHUNK.equals(lastChunk);
    }


}
