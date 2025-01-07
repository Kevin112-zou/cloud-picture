package com.yupi.yupicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.yupi.yupicturebackend.model.dto.picture.PictureQueryRequest;
import com.yupi.yupicturebackend.model.dto.picture.PictureReviewRequest;
import com.yupi.yupicturebackend.model.dto.picture.PictureUploadByBatchRequest;
import com.yupi.yupicturebackend.model.dto.picture.PictureUploadRequest;
import com.yupi.yupicturebackend.model.entity.Picture;
import com.baomidou.mybatisplus.extension.service.IService;
import com.yupi.yupicturebackend.model.entity.User;
import com.yupi.yupicturebackend.model.vo.PictureVO;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Lenovo
 * @description 针对表【picture(图片)】的数据库操作Service
 * @createDate 2025-01-04 13:04:49
 */
public interface PictureService extends IService<Picture> {

    /**
     * 校验图片
     * @param picture 图片信息
     */
    void validPicture(Picture picture);

    /**
     * 上传图片
     * @param inputSource 图片输入源
     * @param pictureUploadRequest 图片上传请求
     * @param loginUser 登录用户
     * @return
     */
    PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser);

    /**
     * 根据条件获取查询对象
     *
     * @param pictureQueryRequest 查询条件
     * @return 图片列表
     */

    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 获取图片包装类（单条）
     *
     * @param picture 图片信息
     * @param request 请求信息
     * @return 脱敏后图片信息
     */
    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    /**
     * 获取图片包装类（分页）
     * @param page 分页信息
     * @param httpServletRequest 请求信息
     * @return 脱敏后图片信息列表
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> page, HttpServletRequest httpServletRequest);

    /**
     * 审核图片
     * @param pictureReviewRequest 审核请求
     * @param user 审核用户
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User user);

    /**
     * 填充审核参数(编辑，上传，更新共用)
     * @param picture 图片信息
     * @param loginUser 登录用户
     */
    void fillReviewParams(Picture picture, User loginUser);

    /**
     *  批量抓取并上传图片
     * @param pictureUploadByBatchRequest 批量上传图片请求
     * @param loginUser 登录用户(admin才能使用)
     * @return 上传成功数量
     */
    Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser);
}
