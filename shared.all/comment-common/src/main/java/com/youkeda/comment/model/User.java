package com.youkeda.comment.model;

/**
 * 用户模型（评论服务视角下的"作者"）
 *
 * 原本由私有 starter 提供，此处补齐。
 * 字段来源：CommentDAO.xml 的 commentModelResultMap
 *   <id property="id" column="user_id"/>
 *   <result column="user_name"  property="userName"/>
 *   <result column="nick_name"  property="nickName"/>
 *   <result column="avatar"     property="avatar"/>
 *
 * 注意：这是评论服务需要的"用户视图"，不是用户服务的完整模型。
 *      第 4 周会用 Feign 从用户服务拿这个数据。
 */
public class User {

    private long id;

    private String userName;

    private String nickName;

    private String avatar;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getNickName() {
        return nickName;
    }

    public void setNickName(String nickName) {
        this.nickName = nickName;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }
}
