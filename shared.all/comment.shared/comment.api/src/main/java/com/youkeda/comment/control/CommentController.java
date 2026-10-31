package com.youkeda.comment.control;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
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
    @SentinelResource(value = "findByRefId", blockHandler = "findByRefIdBlocked")
    public Result<List<Comment>> findByRefId(@RequestParam("refId") String refId) {
        return Result.ok(commentService.findByRefId(refId));
    }

    /**
     * 限流兜底方法（blockHandler）
     *
     * 签名规则很严格，必须同时满足三条，否则 Sentinel 找不到它、会直接抛 FlowException：
     *   ① 参数列表 = 原方法参数 + 最后一个 BlockException
     *   ② 返回值类型和原方法一致
     *   ③ 方法名 = @SentinelResource 的 blockHandler 值
     *
     * 只有被限流/熔断时才会走这里；业务异常不会（那是 fallback 管的事）
     */
    public Result<List<Comment>> findByRefIdBlocked(String refId, BlockException ex) {
        Result<List<Comment>> result = Result.fail("请求太频繁，请稍后再试");
        result.setCode("429");       // 语义上 429 = Too Many Requests，比 500 准确
        return result;
    }

    @GetMapping("/comment/findByUserIds")
    @ResponseBody
    public Result<List<Comment>> findByUserIds(@RequestParam("userIds") List<Long> ids) {
        return Result.ok(commentService.findByUserIds(ids));
    }
}
