package com.yupi.yupicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.yupi.yupicturebackend.model.dto.user.UserQueryRequest;
import com.yupi.yupicturebackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.yupicturebackend.model.vo.LoginUserVO;
import com.yupi.yupicturebackend.model.vo.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

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
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     *  获取脱敏后的登录用户信息
     * @param user 用户账号
     * @return 脱敏信息
     */
    LoginUserVO getLoginUserVo(User user);

    /**
     *  获取脱敏后的用户信息
     * @param user 用户账号
     * @return 脱敏信息
     */
    UserVO getUserVo(User user);

    /**
     *  获取脱敏后的用户信息
     * @return 脱敏后的用户信息列表
     */
    List<UserVO> getUserVoList(List<User> userList);

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

    /**
     * 获取查询条件包装类
     * @param userQueryRequest 查询请求
     * @return 查询条件包装类
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     *  获取当前登录用户信息
     * @param loginUser 登录用户
     * @return 脱敏信息
     */
    LoginUserVO getLoginUserVO(User loginUser);

    /**
     * 判断是否为管理员
     * @param loginUser 登录用户
     * @return 是否为管理员
     */
    public boolean isAdmin(User loginUser);
}
