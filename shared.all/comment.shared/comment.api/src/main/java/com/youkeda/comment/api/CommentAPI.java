package com.youkeda.comment.api;

import com.youkeda.comment.model.Comment;
import com.youkeda.comment.model.Result;
import com.youkeda.comment.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author joe
 */
@Controller
public class CommentAPI {

    @Autowired
    private CommentService commentService;

    @PostMapping("/api/comment/post")
    @ResponseBody
    public Result<Comment> post(@RequestParam("refId") String refId, @RequestParam(value = "parentId") Long parentId,
                                @RequestParam("content") String content, HttpServletRequest request) {
        long userId = (long) request.getSession().getAttribute("userId");
        return commentService.post(refId, userId, parentId, content);
    }

    @GetMapping("/api/comment/query")
    @ResponseBody
    public Result<List<Comment>> query(@RequestParam("refId") String refId) {
        return commentService.query(refId);
    }
}
