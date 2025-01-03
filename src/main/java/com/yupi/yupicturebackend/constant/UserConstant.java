package com.yupi.yupicturebackend.constant;

import java.util.Random;

public interface UserConstant {

    /**
     * 用户登录态键
     */
    String USER_LOGIN_STATE = "user_login";

    //  region 权限

    /**
     * 默认角色
     */
    String DEFAULT_ROLE = "user";

    /**
     * 管理员角色
     */
    String ADMIN_ROLE = "admin";

    /**
     * 默认头像
     */
    String DEFAULT_AVATAR = "https://wx2.sinaimg.cn/orj360/007UpvSSgy1hx7gn5blxcj30sg0sgn6v.jpg";
    /**
     * 默认昵称
     */
    String DEFAULT_NICKNAME = "匿名用户" + new Random().nextInt(1000);
    // endregion
}
