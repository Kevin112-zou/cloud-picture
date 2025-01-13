package com.yupi.yupicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.exception.ThrowUtils;
import com.yupi.yupicturebackend.model.dto.space.SpaceAddRequest;
import com.yupi.yupicturebackend.model.dto.space.SpaceQueryRequest;
import com.yupi.yupicturebackend.model.entity.Space;
import com.yupi.yupicturebackend.model.entity.User;
import com.yupi.yupicturebackend.model.enums.SpaceLevelEnum;
import com.yupi.yupicturebackend.model.vo.SpaceVO;
import com.yupi.yupicturebackend.model.vo.UserVO;
import com.yupi.yupicturebackend.service.SpaceService;
import com.yupi.yupicturebackend.mapper.SpaceMapper;
import com.yupi.yupicturebackend.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Lenovo
 * @description 针对表【space(空间)】的数据库操作Service实现
 * @createDate 2025-01-13 15:13:06
 */
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements SpaceService {

    @Resource
    private UserService userService;

    /**
     * 编程式事务：保证空间在创建成功并保存到数据库后再提交
     */
    @Resource
    private TransactionTemplate transactionTemplate;

    @Override
    public long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        // 1. 填充默认参数
        Space space = new Space();
        BeanUtil.copyProperties(spaceAddRequest, space);
        if (StrUtil.isBlank(space.getSpaceName())) {
            space.setSpaceName("默认空间");
        }
        if (space.getSpaceLevel() == null) {
            space.setSpaceLevel(SpaceLevelEnum.COMMON.getValue()); // 默认普通空间
        }
        // 填充容量和大小
        this.fillSpaceBySpaceLevel(space);
        // 2. 校验参数
        this.validSpace(space, true);
        // 3. 校验权限(非管理员用户只能创建普通空间)
        Long loginUserId = loginUser.getId();
        space.setUserId(loginUserId);
        if (SpaceLevelEnum.COMMON.getValue() != space.getSpaceLevel() && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "非管理员用户只能创建普通空间");
        }
        // 4. 控制同一用户只能创建一个私有空间(根据用户id上加锁)
        String lock = String.valueOf(loginUserId).intern();
        Long newSpaceId = transactionTemplate.execute(status -> {
            synchronized (lock) {
                // 判断是否存在私有空间
                Space exists = this.lambdaQuery().eq(Space::getUserId, loginUserId).one();
                if (exists != null) {
                    throw new BusinessException(ErrorCode.OPERATION_ERROR, "同一用户只能创建一个私有空间");
                }
                // 创建空间
                boolean save = this.save(space);
                ThrowUtils.throwIf(!save, ErrorCode.SYSTEM_ERROR, "创建空间失败");
                return space.getId();
            }
        });
        return newSpaceId;
    }

    @Override
    public void validSpace(Space space, boolean isAdd) {
        // 校验逻辑
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);
        // 从对象取值
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();

        // 获取枚举对象（空间级别）
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        // 创建时校验

        if (isAdd) {
            if (StrUtil.isBlank(spaceName)) {
                ThrowUtils.throwIf(StrUtil.isBlank(spaceName), ErrorCode.PARAMS_ERROR, "空间名不能为空");
            }
            if (spaceLevel == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不能为空");
            }
        }
        // 修改数据时校验
        if (StrUtil.isBlank(spaceName) && spaceName.length() > 30) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名不能超过30个字符");
        }
        // 校验空间级别是否正确
        if (spaceLevel != null && spaceLevelEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不存在");
        }
    }

    @Override
    public QueryWrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        if (spaceQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = spaceQueryRequest.getId();
        Long userId = spaceQueryRequest.getUserId();
        String spaceName = spaceQueryRequest.getSpaceName();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        String sortField = spaceQueryRequest.getSortField();
        String sortOrder = spaceQueryRequest.getSortOrder();

        // 拼接查询条件
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(spaceName), "spaceName", spaceName);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceLevel), "spaceLevel", spaceLevel);
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        // 1. 对象转封装类
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        // 2. 关联查询用户信息
        Long userId = space.getUserId();
        if (userId != null && userId > 0) {
            // 查询用户信息，封装到pictureVO中
            User user = userService.getById(userId);
            UserVO userVo = userService.getUserVo(user); // 封装脱敏后的用户信息
            spaceVO.setUser(userVo);
        }
        return spaceVO;
    }

    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest httpServletRequest) {
        List<Space> spaceList = spacePage.getRecords(); //
        //
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
        if (CollUtil.isEmpty(spaceList)) {
            return spaceVOPage;
        }
        // 对象列表 => 封装对象列表
        List<SpaceVO> spaceVOList = spaceList.stream().map(SpaceVO::objToVo).collect(Collectors.toList());
        // 1. 关联查询用户信息
        Set<Long> userIdSet = spaceList.stream().map(Space::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        spaceVOList.forEach(spaceVO -> {
            Long userId = spaceVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            spaceVO.setUser(userService.getUserVo(user));
        });
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }

    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        if (spaceLevelEnum != null) {
            long maxCount = spaceLevelEnum.getMaxCount();
            // 如果空间最大容量为空，则填充默认值(保证管理员在创建空间时可以自定义更大的容量，更加灵活)
            if (space.getMaxCount() == null) {
                space.setMaxCount(maxCount);
            }
            long maxSize = spaceLevelEnum.getMaxSize();
            if (space.getMaxSize() == null) {
                space.setMaxSize(maxSize);
            }
        }
    }

}




