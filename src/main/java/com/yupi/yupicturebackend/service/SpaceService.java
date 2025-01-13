package com.yupi.yupicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.yupicturebackend.model.dto.space.SpaceAddRequest;
import com.yupi.yupicturebackend.model.dto.space.SpaceQueryRequest;
import com.yupi.yupicturebackend.model.entity.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.yupicturebackend.model.entity.User;
import com.yupi.yupicturebackend.model.vo.SpaceVO;
import org.springframework.scheduling.annotation.Async;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Lenovo
 * @description 针对表【space(空间)】的数据库操作Service
 * @createDate 2025-01-13 15:13:06
 */
public interface SpaceService extends IService<Space> {

    /**
     * 创建空间
     * @param spaceAddRequest 空间添加请求
     * @param loginUser 登录用户
     * @return 空间id
     */
    long addSpace(SpaceAddRequest spaceAddRequest,User loginUser);
    /**
     * 校验空间
     *
     * @param space 空间信息
     */
    void validSpace(Space space,boolean isAdd);


    /**
     * 根据条件获取查询对象
     *
     * @param spaceQueryRequest 查询条件
     * @return 空间列表
     */

    QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    /**
     * 获取空间包装类（单条）
     *
     * @param space   空间信息
     * @param request 请求信息
     * @return 脱敏后空间信息
     */
    SpaceVO getSpaceVO(Space space, HttpServletRequest request);

    /**
     * 获取空间包装类（分页）
     *
     * @param page               分页信息
     * @param httpServletRequest 请求信息
     * @return 脱敏后空间信息列表
     */
    Page<SpaceVO> getSpaceVOPage(Page<Space> page, HttpServletRequest httpServletRequest);

    /**
     * 根据空间等级填充空间的容量和大小（支持前端管理指定大小）
     *
     * @param space     空间信息
     */
    void fillSpaceBySpaceLevel(Space space);



}
