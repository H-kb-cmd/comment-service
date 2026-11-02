package com.youkeda.comment.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.youkeda.comment.dao.CommentDAO;
import com.youkeda.comment.dataobject.CommentDO;
import com.youkeda.comment.exception.SensitiveWordException;
import com.youkeda.comment.model.Comment;
import com.youkeda.comment.model.Paging;
import com.youkeda.comment.model.Result;
import com.youkeda.comment.model.User;
import com.youkeda.comment.service.CommentService;
import com.youkeda.comment.service.UserService;
import com.youkeda.comment.util.CommentDOUtil;
import com.youkeda.comment.util.SensitiveWordFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CommentServiceImpl implements CommentService {

    @Autowired
    private CommentDAO commentDAO;

    @Autowired
    private UserService userService;

    /** 敏感词过滤器：启动时建好前缀树，每条评论只做一次线性扫描 */
    @Autowired
    private SensitiveWordFilter sensitiveWordFilter;

    @PostConstruct
    public void init() {
        System.out.println("CommentServiceImpl初始化完毕");
    }

    @Override
    public Result<Comment> post(String refId, long userId, long parentId, String content) {
        Result<Comment> result = new Result<>();
        if (StringUtils.isEmpty(refId) || userId == 0 || StringUtils.isEmpty(content)) {
            result.setCode("500");
            result.setMessage("refId、userId、content 不能为空");
            return result;
        }

        // ★ 敏感词检测：必须在 HTML 转义之前做
        //   因为转义会把 < 变成 &lt;、& 变成 &amp;，词库里的词如果含这些符号就再也匹配不上了
        try {
            checkSensitive(content);
        } catch (SensitiveWordException e) {
            result.setCode("400");
            result.setMessage(e.getMessage());
            return result;
        }

        String body = StringEscapeUtils.escapeHtml4(content);

        CommentDO commentDO = new CommentDO();
        commentDO.setUserId(userId);
        commentDO.setRefId(refId);
        commentDO.setParentId(parentId);
        commentDO.setContent(body);
        commentDAO.insert(commentDO);

        result.setData(commentDO.toModel());

        return result;
    }

    @Override
    public Result<List<Comment>> query(String refId) {

        Result<List<Comment>> result = new Result<>();
        //查询所有的评论记录包含回复的
        List<Comment> comments = buildComments(commentDAO.findByRefId(refId));
        //构建 map 结构
        Map<Long, Comment> commentMap = new HashMap<>();
        //初始化一个虚拟根节点，0 可以对应的是所有一级评论的父亲
        commentMap.put(0L, new Comment());
        //把所有的评论转换为 map 数据
        comments.forEach(comment -> commentMap.put(comment.getId(), comment));
        // 再次遍历评论数据
        comments.forEach(comment -> {
            //得到父评论
            Comment parent = commentMap.get(comment.getParentId());
            if (parent != null) {
                // 初始化 children 变量
                if (parent.getChildren() == null) {
                    parent.setChildren(new ArrayList<>());
                }
                // 在父评论里添加回复数据
                parent.getChildren().add(comment);
            }
        });
        // 得到所有的一级评论
        List<Comment> data = commentMap.get(0L).getChildren();

        result.setSuccess(true);
        result.setData(data);

        return result;
    }

    public int batchAdd(List<Comment> commentDOs) {
        List<CommentDO> dos = new ArrayList<>();

        for (Comment comment : commentDOs) {
            checkSensitive(comment.getContent());   // 逐条校验：任一条命中就抛异常，整批不会写库
            dos.add(CommentDOUtil.toDO(comment));
        }

        return commentDAO.batchAdd(dos);
    }

    public List<Comment> findAll() {
        return buildComments(commentDAO.findAll());
    }

    /**
     * 把 CommentDO 列表补全成 Comment 列表（填充 author）
     *
     * 抽成独立方法的原因：
     *   findAll() 和 findPage() 都需要"补作者"这一步，
     *   如果不抽出来，两处会写一模一样的十几行代码。
     */
    private List<Comment> buildComments(List<CommentDO> dos) {
        // ① 收集所有 userId
        List<Long> userIds = new ArrayList<>();
        for (CommentDO commentDO : dos) {
            userIds.add(commentDO.getUserId());
        }

        // ② 批量查用户：1 次查询查完所有人，不是每条评论查一次
        List<User> userModels = userService.findByIds(userIds);

        // ③ 建索引 userId -> User，避免后面嵌套循环（O(n²)）
        Map<Long, User> usersMap = new HashMap<>();
        for (User userModel : userModels) {
            usersMap.put(userModel.getId(), userModel);
        }

        // ④ 逐条补全
        List<Comment> results = new ArrayList<>();
        for (CommentDO commentDO : dos) {
            Comment comment = commentDO.toModel();
            User author = usersMap.get(comment.getAuthor().getId());
            if (author != null) {
                comment.setAuthor(author);
            }
            results.add(comment);
        }
        return results;

    }

    /**
     * 敏感词校验：命中就抛异常，由调用方决定怎么处理
     *
     * 抽成独立方法的原因：post() 和 insert() 两个写入口都要校验，
     * 但不能复制两遍逻辑，否则以后改规则（比如改成"打码放行"）要改两处。
     */
    private void checkSensitive(String content) {
        String hitWord = sensitiveWordFilter.findFirst(content);
        if (hitWord != null) {
            // 注意：不要把命中的词返回给前端，否则等于把词库内容告诉对方，改个同音字就能绕过
            System.out.println("[敏感词] 拦截一条评论，命中: " + hitWord);
            throw new SensitiveWordException("评论包含敏感词，请修改后再发布");
        }
    }

    public int insert(Comment comment) {
        checkSensitive(comment.getContent());       // ★ 真正的 HTTP 写入口，校验必须放在这里
        CommentDO commentDO = CommentDOUtil.toDO(comment);
        int rows = commentDAO.insert(commentDO);
        // ★ 把数据库生成的自增主键回填给 model
        //   不回填的话，Controller 返回给前端的 id 永远是 0，
        //   前端拿到的评论没法回复、没法删除（因为它不知道自己的 id）
        comment.setId(commentDO.getId());
        return rows;
    }

    public int update(Comment comment) {
        return commentDAO.update(CommentDOUtil.toDO(comment));
    }

    public int delete(long id) {
        return commentDAO.delete(id);
    }

    public List<Comment> findByRefId(String refId) {

        return buildComments(commentDAO.findByRefId(refId));
    }

    public List<Comment> findByUserIds(List<Long> ids) {
        List<Comment> results = new ArrayList<>();
        List<CommentDO> dos = commentDAO.findByUserIds(ids);

        List<User> userModels = userService.findByIds(ids);

        Map<Long, User> usersMap = new HashMap<>();

        for (User userModel : userModels) {
            usersMap.put(userModel.getId(), userModel);
        }

        for (CommentDO commentDO : dos) {
            Comment comment = commentDO.toModel();
            User author = usersMap.get(comment.getAuthor().getId());
            if (author != null) {
                comment.setAuthor(author);
            }

            results.add(comment);
        }

        return results;
    }

    @Override
    public Paging<Comment> findPage(int pageNum, int pageSize) {
        PageHelper.startPage(pageNum, pageSize);

        List<CommentDO> dos = commentDAO.findAll();

        PageInfo<CommentDO> info = new PageInfo<>(dos);

        List<Comment> comments = buildComments(dos);

        return new Paging<>(info.getPageNum(), info.getPageSize(), info.getPages(), info.getTotal(), comments);
    }
}
