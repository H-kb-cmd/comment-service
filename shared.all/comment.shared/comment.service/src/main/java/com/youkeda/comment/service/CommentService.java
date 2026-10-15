package com.youkeda.comment.service;

import com.youkeda.comment.model.Comment;
import com.youkeda.comment.model.Result;

import java.util.List;

public interface CommentService {

    /**
     * 发布评论
     *
     * @param refId    外部 ID
     * @param userId   用户 ID
     * @param parentId 父评论 ID
     * @param content  评论内容
     * @return
     */
    public Result<Comment> post(String refId, long userId, long parentId, String content);


    /**
     * 查询评论
     *
     * @param refId
     * @return
     */
    public Result<List<Comment>> query(String refId);

    public int batchAdd(List<Comment> commentDOs);

    public List<Comment> findAll();

    public int insert(Comment commentDO);

    public int update(Comment commentDO);

    public int delete(long id);

    public List<Comment> findByRefId(String refId);

    public List<Comment> findByUserIds(List<Long> ids);
}
