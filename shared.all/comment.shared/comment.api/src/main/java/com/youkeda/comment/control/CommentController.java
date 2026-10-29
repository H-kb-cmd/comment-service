package com.youkeda.comment.control;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.youkeda.comment.model.Comment;
import com.youkeda.comment.model.Paging;
import com.youkeda.comment.model.Result;
import com.youkeda.comment.service.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class CommentController {

    @Autowired
    private CommentService commentService;

    @GetMapping("/comments")
    @ResponseBody
    public Result<Paging<Comment>> getAll(@RequestParam(value = "pageNum", required = false) Integer pageNum,
                                  @RequestParam(value = "pageSize", required = false) Integer pageSize) {
        if (pageNum == null) { pageNum = 1; }
        if (pageSize == null) { pageSize = 15; }

        return Result.ok(commentService.findPage(pageNum, pageSize));
    }


    @PostMapping("/comment")
    @ResponseBody
    public Result<Comment> save(@RequestBody Comment comment) {
        commentService.insert(comment);
        return Result.ok(comment);
    }

    @PostMapping("/comment/batchAdd")
    @ResponseBody
    public Result<List<Comment>> batchAdd(@RequestBody List<Comment> comments) {
        commentService.batchAdd(comments);
        return Result.ok(comments);
    }

    @PostMapping("/comment/update")
    @ResponseBody
    public Result<Comment> update(@RequestBody Comment comment) {
        commentService.update(comment);
        return Result.ok(comment);
    }

    @GetMapping("/comment/del")
    @ResponseBody
    public Result<Boolean> delete(@RequestParam("id") Long id) {
        int rows = commentService.delete(id);
        if (rows > 0) {
            return Result.ok(true);
        }
        return Result.fail("评论不存在或已被删除");
    }

    @GetMapping("/comment/findByRefId")
    @ResponseBody
    public Result<List<Comment>> findByRefId(@RequestParam("refId") String refId) {
        return Result.ok(commentService.findByRefId(refId));
    }

    @GetMapping("/comment/findByUserIds")
    @ResponseBody
    public Result<List<Comment>> findByUserIds(@RequestParam("userIds") List<Long> ids) {
        return Result.ok(commentService.findByUserIds(ids));
    }
}
