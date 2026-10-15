package com.youkeda.comment.util;

import com.youkeda.comment.dataobject.CommentDO;
import com.youkeda.comment.model.Comment;
import com.youkeda.comment.model.User;

public class CommentDOUtil {

    /**
     * 模型转换为 DO
     *
     * @param comment
     * @return
     */
    public static CommentDO toDO(Comment comment) {
        CommentDO commentDO = new CommentDO();
        commentDO.setId(comment.getId());
        commentDO.setGmtCreated(comment.getGmtCreated());
        commentDO.setGmtModified(comment.getGmtModified());

        if (comment.getAuthor() != null) {
            commentDO.setUserId(comment.getAuthor().getId());
        }

        commentDO.setParentId(comment.getParentId());
        commentDO.setContent(comment.getContent());
        commentDO.setRefId(comment.getRefId());
        return commentDO;
    }
}
