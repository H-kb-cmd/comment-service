package com.youkeda.comment.service;

import com.youkeda.comment.model.Result;
import com.youkeda.comment.model.User;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
@Component
public class UserFallbackService implements UserService{
    @Override
    public Result<List<User>> findByIdsRemote(List<Long> ids) {
        List<User> users = new ArrayList<>();
        if (ids != null) {
            for(Long id : ids){
                User user = new User();
                user.setId(id);
                user.setUserName("unknown");
                user.setNickName("未命名用户");
                users.add(user);
            }
        }
        return Result.ok(users);
    }
}
