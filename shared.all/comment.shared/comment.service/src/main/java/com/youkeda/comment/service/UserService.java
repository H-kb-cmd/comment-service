package com.youkeda.comment.service;

import com.youkeda.comment.model.Result;
import com.youkeda.comment.model.User;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Collections;
import java.util.List;

/**
 * 用户查询服务
 *
 * 原本由私有 starter（user-spring-boot-starter）提供实现，此处补齐。
 *
 * ⭐ 这个接口是第 4 周改造的关键位置：
 *    现在：本地实现 → 直接查本库的 user 表
 *    Day 4：改成 @FeignClient → 调用独立的用户服务
 *
 *    接口不变、调用方不变，只换实现 —— 这就是"面向接口编程"的价值。
 */
@FeignClient(value = "user-service",fallback = UserFallbackService.class)
public interface UserService {

    /**
     * 真正的远程调用：POST http://user-service/user/ids  body: [1,2,3]
     *
     * 返回 Result 包装是必须的 —— 服务端返回的是 {"code":...,"data":[...]}，
     * 如果这里声明成 List<User>，Jackson 会试图把整个 JSON 映射成数组，直接失败。
     */
    @PostMapping("/user/ids")
    Result<List<User>> findByIdsRemote(@RequestBody List<Long> ids);
    /**
     * 业务方真正调用的方法（default 方法，Feign 不会代理它）
     *
     * 把 Result 拆包，让上层拿到的还是干净的 List<User>。
     */
    default List<User> findByIds(List<Long> ids){
        Result<List<User>> result = findByIdsRemote(ids);

            if (result==null||result.getData()==null){
                return Collections.emptyList();
            }
            return result.getData();

    }
}
