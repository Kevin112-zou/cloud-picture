package com.yupi.yupicturebackend.service;

import com.yupi.yupicturebackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.yupicturebackend.model.vo.LoginUserVo;
import lombok.Data;

import javax.servlet.http.HttpServletRequest;

/**
* @author Lenovo
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2024-12-28 11:57:33
*/

public interface UserService extends IService<User> {

    /**
     * 用户注册方法
     * @param userAccount 用户账号
     * @param userPassword 用户密码
     * @param checkPassword 校验密码
     * @return 用户id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 获取加密后的密码
     * @param userPassword 用户密码
     * @return 加密后的密码
     */
    String getEncryptPassword(String userPassword);

    /**
     * 用户登录方法
     * @param userAccount 用户账号
     * @param userPassword 用户密码
     * @return
     */
    LoginUserVo userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     *  获取脱敏后的登录信息
     * @param user 用户账号
     * @return 脱敏信息
     */
    LoginUserVo getLoginUserVo(User user);

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获取当前登录用户
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);
}
