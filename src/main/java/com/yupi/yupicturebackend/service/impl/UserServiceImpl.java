package com.yupi.yupicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yupicturebackend.constant.UserConstant;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.mapper.UserMapper;
import com.yupi.yupicturebackend.model.dto.user.UserQueryRequest;
import com.yupi.yupicturebackend.model.entity.User;
import com.yupi.yupicturebackend.model.enums.UserRoleEnum;
import com.yupi.yupicturebackend.model.vo.LoginUserVo;
import com.yupi.yupicturebackend.model.vo.UserVo;
import com.yupi.yupicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author Lenovo
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2024-12-28 11:57:33
*/
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{


    /**
     * 用户注册方法
     * @param userAccount 用户账号(唯一)
     * @param userPassword 用户密码
     * @param checkPassword 确认密码
     * @return
     */
    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 1. 校验参数
        if(StrUtil.hasBlank(userAccount, userPassword, checkPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if(userAccount.length() < 4){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if(userPassword.length() < 8 || checkPassword.length() < 8){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        if (!userPassword.equals(checkPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        // 2. 检查用户账号是否和数据库中重复
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount",userAccount);
        Long count = this.baseMapper.selectCount(queryWrapper);
        if(count > 0){
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"账号重复");
        }
        // 3. 密码加密
        String encryptPassword = getEncryptPassword(userPassword);
        // 4. 插入数据库
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserRole(UserRoleEnum.USER.getValue());
        user.setUserPassword(encryptPassword);
        user.setUserName("小黑字");
        boolean result = this.save(user);
        if(!result){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"注册失败，数据库错误！");
        }
        // 为什么能直接拿到uid,原因是mybatis中save方法会自动回填id
        return user.getId();
    }
    /**
     * 获取加密后的密码
     * @param userPassword 用户密码
     * @return  加密后的密码
     */
    @Override
    public String getEncryptPassword(String userPassword){
        // 加盐，混淆密码
        final String SALT = "ztlztl";
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }
    /**
     *  获取脱敏后的登录信息
     * @param user 用户账号
     * @return 脱敏信息
     */
    @Override
    public LoginUserVo getLoginUserVo(User user) {
        if(user == null){
            return null;
        }
        LoginUserVo loginUserVo = new LoginUserVo();
        BeanUtil.copyProperties(user,loginUserVo);
        return loginUserVo;
    }

    /**
     *  获取脱敏后的用户信息
     * @param user 用户账号
     * @return 脱敏信息
     */
    @Override
    public UserVo getUserVo(User user) {
        if(user == null){
            return null;
        }
        UserVo userVo = new UserVo();
        BeanUtil.copyProperties(user,userVo);
        return userVo;
    }

    /**
     *  获取脱敏后的用户信息列表
     * @param userList 用户列表
     * @return 脱敏后的用户信息列表
     */
    @Override
    public List<UserVo> getUserVoList(List<User> userList) {
        if(CollUtil.isEmpty(userList)){
            return null;
        }
        List<UserVo> userVoList = CollUtil.newArrayList();
        for(User user : userList){
            UserVo userVo = getUserVo(user);
            if(userVo != null){
                userVoList.add(userVo);
            }
        }
        // stream写法
        // userVoList = userList.stream().map(this::getUserVo).collect(Collectors.toList());
        return userVoList;
    }

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 1. 先判断是否登录
        Object attribute = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if(attribute == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 2. 移除登录状态
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        return true;
    }

    /**
     * 获取当前登录用户
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 1. 判断是否登录
        Object object = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) object;
        if(object == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 从数据库查询（追求性能的话可以直接返回上述结果）
        // 2. 查询数据库
        Long id = currentUser.getId();
        User user = this.getById(id);
        if(user == null){
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return user;
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);  // 这里的id是前端传过来的，所以需要判断是否为空
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole); // 这里的userRole是前端传过来的，所以需要判断是否为空
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount); // 这里的userAccount是前端传过来的，所以需要判断是否为空
        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName); // 这里的userName是前端传过来的，所以需要判断是否为空
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile); // 这里的userProfile是前端传过来的，所以需要判断是否为空
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }


    @Override
    public LoginUserVo userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if(StrUtil.hasBlank(userAccount, userPassword)){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if(userAccount.length() < 4){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号错误");
        }
        if(userPassword.length() < 8){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码错误");
        }
        // 2. 对用户密码加密
        String encryptPassword = getEncryptPassword(userPassword);
        // 3. 查询数据库中的用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount",userAccount);
        queryWrapper.eq("userPassword",encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);

        // 不存在抛异常
        if(user == null){
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"用户不存在或者用户密码错误");
        }
        // 4. 保存用户的登录状态(key: value)  key(String) value(user)
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE,user);
        return this.getLoginUserVo(user);
    }

}




