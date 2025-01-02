package com.yupi.yupicturebackend.controller;

import cn.hutool.db.PageResult;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.sun.org.apache.xpath.internal.operations.Bool;
import com.yupi.yupicturebackend.annotation.AuthCheck;
import com.yupi.yupicturebackend.common.BaseResponse;
import com.yupi.yupicturebackend.common.DeleteRequest;
import com.yupi.yupicturebackend.common.PageRequest;
import com.yupi.yupicturebackend.common.ResultUtils;
import com.yupi.yupicturebackend.constant.UserConstant;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.exception.ThrowUtils;
import com.yupi.yupicturebackend.model.dto.user.*;
import com.yupi.yupicturebackend.model.entity.User;
import com.yupi.yupicturebackend.model.vo.LoginUserVo;
import com.yupi.yupicturebackend.model.vo.UserVo;
import com.yupi.yupicturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;
    /**
     * 用户注册
     * @return 用户id
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        long result = userService.userRegister(userAccount, userPassword, checkPassword);
        return ResultUtils.success(result);
    }

    /**
     * 用户登录
     * @return 用户id
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVo> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        LoginUserVo loginUserVo = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(loginUserVo);
    }

    /**
     * 用户注销
     * @return 用户id
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(@RequestBody HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 根据id获取当前用户（仅限管理员）
     * @param id 用户id
     * @return 用户信息
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE) // 限定只有管理员可以调用
    public  BaseResponse<User> getUserById(Long id) {
        ThrowUtils.throwIf(id == null || id < 0, ErrorCode.PARAMS_ERROR);
        // 1. 查询用户是否存在
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        // 2. 返回用户信息
        return ResultUtils.success(user);
    }

    /**
     * 根据id获取脱敏后的用户信息
     * @param id 用户id
     * @return 脱敏后的用户信息
     */
    @GetMapping("/get/vo")
    public  BaseResponse<UserVo> getUserVoById(Long id) {

        BaseResponse<User> response = getUserById(id);
        // 封装脱敏后的用户信息
        User user = response.getData();
        return ResultUtils.success(userService.getUserVo(user));
    }
    /**
     * 删除用户
     * @param deleteRequest 用户id
     * @return 删除结果
     */
    @PostMapping("/delete")
    public  BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        if(deleteRequest == null || deleteRequest.getId() <= 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        boolean result = userService.removeById(deleteRequest.getId());

        return ResultUtils.success(result);
    }
    /**
     * 创建用户
     * @return 用户id
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE) // 限定只有管理员可以调用
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);

        User user = new User();
        BeanUtils.copyProperties(userAddRequest, user);
        // 1. 设置默认（加密）密码
        final  String DEFAULT_PASSWORD = "12345678";
        String encryptPassword = userService.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);

        // 2. 保存用户
        boolean result = userService.save(user);

        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        return ResultUtils.success(user.getId());
    }

    /**
     * 更新用户信息
     * @return 用户id
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE) // 限定只有管理员可以调用
    public BaseResponse<Boolean> addUser(@RequestBody UserUpdateRequest userUpdateRequest) {

        // 1. 校验参数
        ThrowUtils.throwIf(userUpdateRequest == null, ErrorCode.PARAMS_ERROR);

        // 2. 更新用户
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        // 3. 返回结果
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        return ResultUtils.success(true);
    }

    /**
     * 分页查询用户列表（仅限管理员）
     * @param userQueryRequest 分页查询请求
     * @return 脱敏后的用户信息
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE) // 限定只有管理员可以调用
    public BaseResponse<Page<UserVo>> pageQueryUser(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = userQueryRequest.getCurrent(); // 获取当前页码
        long pageSize = userQueryRequest.getPageSize();// 获取页面大小
        // 封装分页查询条件
        Page<User> userPage = userService.page(new Page<>(current, pageSize), userService.getQueryWrapper(userQueryRequest));
        Page<UserVo> userVoPage = new Page<>(current,pageSize, userPage.getTotal());

        // List<User> records = userPage.getRecords();
        List<UserVo> userVoList = userService.getUserVoList(userPage.getRecords());
        userVoPage.setRecords(userVoList);
        return ResultUtils.success(userVoPage);
    }
}
