package com.yupi.yupicturebackend.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.exception.ThrowUtils;
import com.yupi.yupicturebackend.manager.CosManager;
import com.yupi.yupicturebackend.manager.upload.FilePictureUpload;
import com.yupi.yupicturebackend.manager.upload.PictureUploadTemplate;
import com.yupi.yupicturebackend.manager.upload.UrlPictureUpload;
import com.yupi.yupicturebackend.model.dto.file.UploadPictureResult;
import com.yupi.yupicturebackend.model.dto.picture.PictureQueryRequest;
import com.yupi.yupicturebackend.model.dto.picture.PictureReviewRequest;
import com.yupi.yupicturebackend.model.dto.picture.PictureUploadByBatchRequest;
import com.yupi.yupicturebackend.model.dto.picture.PictureUploadRequest;
import com.yupi.yupicturebackend.model.entity.Picture;
import com.yupi.yupicturebackend.model.entity.User;
import com.yupi.yupicturebackend.model.enums.PictureReviewStatusEnum;
import com.yupi.yupicturebackend.model.vo.PictureVO;
import com.yupi.yupicturebackend.model.vo.UserVO;
import com.yupi.yupicturebackend.service.PictureService;
import com.yupi.yupicturebackend.mapper.PictureMapper;
import com.yupi.yupicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author Lenovo
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-01-04 13:04:49
 */
@Service
@Slf4j
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture>
        implements PictureService {


    @Resource
    private UserService userService;

    @Resource
    private FilePictureUpload filePictureUpload;

    @Resource
    private UrlPictureUpload urlPictureUpload;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CosManager cosManager;
    /**
     * 本地缓存
     */
    private final LoadingCache<String, String> LOCAL_CACHE = Caffeine.newBuilder()
            .initialCapacity(1024) // 初始容量
            .maximumSize(10000) // 最大容量
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build(key -> loadDataFromDataSource(key));

    private String loadDataFromDataSource(String key) {
        // 这里编写从实际数据源获取数据的逻辑
        // 例如从数据库查询数据，这里只是简单返回一个示例字符串
        return "Data for key: " + key;
    }

    public void validPicture(Picture picture) {
        // 校验逻辑
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String name = picture.getName();
        String introduction = picture.getIntroduction();
        // 修改数据时，id 不能为空
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "图片 id 不能为空");
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url过长");
        }
        if (StrUtil.isNotBlank(name)) {
            ThrowUtils.throwIf(name.length() > 128, ErrorCode.PARAMS_ERROR, "图片名称过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "图片简介过长");
        }

    }

    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        // 校验参数
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        // 判断是新增还是更新
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        // 如果是更新，则查询数据库是否存在该id的图片
        if (pictureId != null) {
            Picture oldPicture = this.getById(pictureId);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
            // 仅本人和管理员可以更新图片
            if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
        // 上传图片/public桶，得到图片信息
        String uploadPathPrefix = String.format("public/%s", loginUser.getId()); // 图片上传路径前缀

        PictureUploadTemplate pictureUploadTemplate = filePictureUpload; // 默认使用文件上传方式
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload; // 使用url上传方式
        }
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(inputSource, uploadPathPrefix);

        // 构造要入库的图片信息
        Picture picture = new Picture();
        BeanUtils.copyProperties(uploadPictureResult, picture);


        // 支持外层上传图片名称，否则使用默认的图片名
        String picName = uploadPictureResult.getPicName();
        if (pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        }
        picture.setName(picName);
        picture.setUserId(loginUser.getId());

        // 自定义标签，支持传入多个标签
        if (pictureUploadRequest != null && CollUtil.isNotEmpty(pictureUploadRequest.getTags())) {
            picture.setTags(JSONUtil.toJsonStr(pictureUploadRequest.getTags()));
        }


        // 如果pictureId不为空，则更新,否则新增

        if (pictureId != null) {

            // 如果是更新，则需要设置id和编辑时间
            picture.setId(pictureId);
            picture.setEditTime(new Date());
        }
        // 插入数据库之前，添加审核参数
        this.fillReviewParams(picture, loginUser);
        // 保存数据到数据库
        boolean result = this.saveOrUpdate(picture);// 保存或更新图片信息
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "上传图片失败，数据库操作失败");

        return PictureVO.objToVo(picture); // 返回图片VO对象
    }


    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
//        Long reviewTime = pictureQueryRequest.getReviewTime();
        // 从多字段中搜索
        if (StrUtil.isNotBlank(searchText)) {
            // 需要拼接查询条件
            // 保证or只对这个字段生效，所以使用and嵌套
            // and (searchText like "%searchText%" or introduction like "%searchText%")
            queryWrapper.and(qw -> qw.like("name", searchText)
                    .or()
                    .like("introduction", searchText)
            );
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
        // JSON 数组查询
        if (CollUtil.isNotEmpty(tags)) {
            // and (tags like "\"java\"") or (tags like "\"python\"") or ...
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        // 1. 对象转封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
        // 2. 关联查询用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            // 查询用户信息，封装到pictureVO中
            User user = userService.getById(userId);
            UserVO userVo = userService.getUserVo(user); // 封装脱敏后的用户信息
            pictureVO.setUser(userVo);
        }
        return pictureVO;
    }

    /**
     * 分页获取图片封装
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {

        List<Picture> pictureList = picturePage.getRecords();
        //
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        // 对象列表 => 封装对象列表
        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::objToVo).collect(Collectors.toList());
        // 1. 关联查询用户信息
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVO.setUser(userService.getUserVo(user));
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User user) {
        // 1. 校验参数
        ThrowUtils.throwIf(pictureReviewRequest == null, ErrorCode.PARAMS_ERROR);
        Long id = pictureReviewRequest.getId(); // 图片id
        Integer reviewStatus = pictureReviewRequest.getReviewStatus(); // 审核状态
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);// 审核状态枚举

        String reviewMessage = pictureReviewRequest.getReviewMessage();
        // 不允许再将图片设置为待审核状态
        if (id == null || reviewStatusEnum == null || PictureReviewStatusEnum.REVIEWING.equals(reviewStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 2. 判断图片是否存在（去数据库中查找）
        Picture oldPicture = this.getById(id);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 3. 校验审核状态是否重复
        if (oldPicture.getReviewStatus().equals(reviewStatus)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "请务重复审核！");
        }
        // 4. 更新审核状态
        Picture picture = new Picture();
        BeanUtil.copyProperties(pictureReviewRequest, picture);
        picture.setReviewerId(user.getId()); // 设置审核人id
        picture.setReviewTime(new Date()); // 设置审核时间
        boolean res = this.updateById(picture);
        ThrowUtils.throwIf(!res, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        if (userService.isAdmin(loginUser)) {
            // 管理员自动过审 + 设置审核通过信息
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewTime(new Date());
            picture.setReviewMessage("管理员自动审核通过");
        } else {
            // 非管理员用户，无论是编辑还是新增，都设置为待审核
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());

        }
    }

    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        String searchText = pictureUploadByBatchRequest.getSearchText();
        // 格式化数量
        Integer count = pictureUploadByBatchRequest.getCount();
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "最多 30 条");

        // 自定义图片名称
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        if (StrUtil.isBlank(namePrefix)) {
            namePrefix = searchText;// 如果没有自定义，就用搜索文本作为前缀
        }


        // 要抓取的地址
        int first = 1;
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1&first=%s&count=%s", searchText, first, count);
        Document document;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取页面失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败");
        }
        // 解析内容
        Element div = document.getElementsByClass("dgControl").first();
        if (ObjUtil.isNull(div)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取元素失败");
        }
        Elements imgElementsList = div.select("img.mimg");
        // 遍历图片元素，依次处理上传图片
        int uploadedCount = 0;
        for (Element img : imgElementsList) {
            String fileUrl = img.attr("src");
            if (StrUtil.isEmpty(fileUrl)) {
                log.info("当前图片地址为空，跳过：{} ", fileUrl);
                continue;
            }
            // 处理图片的地址，防止转义或者和对象存储冲突
            int questionMarkIndex = fileUrl.indexOf("?");
            if (questionMarkIndex > -1) {
                fileUrl = fileUrl.substring(0, questionMarkIndex);
            }
            // 上传图片
            PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
            pictureUploadRequest.setFileUrl(fileUrl);
            pictureUploadRequest.setPicName(namePrefix + (uploadedCount + 1));
            pictureUploadRequest.setTags(pictureUploadByBatchRequest.getTags());

            try {
                PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
                log.info("上传图片成功：if = {}", pictureVO.getId());
                // 上传成功，计数器加一
                uploadedCount++;
                first += count;
            } catch (Exception e) {
                log.error("上传图片失败", e);
                continue;
            }
            if (uploadedCount >= count) {
                break;
            }
        }
        return uploadedCount;
    }

    @Override
    public Page<PictureVO> listPictureVOByPageWithCache(PictureQueryRequest pictureQueryRequest, HttpServletRequest request) {

        // 构建key
        String questCondition = JSONUtil.toJsonStr(pictureQueryRequest);
        // 使用md5加密作为key, 防止key太长导致redis报错
        String hashKey = DigestUtils.md5DigestAsHex(questCondition.getBytes());
        // 构建key  = "项目名称:方法名:参数"
        String cacheKey = "picture:listPictureVOByPage:" + hashKey;

        // 使用多级缓存 一级：本地缓存，二级：redis缓存

        // 1. 从本地缓存查询
        String localCache = LOCAL_CACHE.getIfPresent(cacheKey);
        if (localCache != null) {
            // 缓存命中，直接返回
            Page<PictureVO> cachePage = JSONUtil.toBean(localCache, Page.class);
            return cachePage;
        }
        // 2. 本地缓存未命中，从redis查询
        ValueOperations<String, String> opsForValue = stringRedisTemplate.opsForValue();
        String cacheValue = opsForValue.get(cacheKey);
        if (cacheValue != null) {
            // 缓存命中，先更新本地缓存
            LOCAL_CACHE.put(cacheKey, cacheValue);
            Page<PictureVO> cachePage = JSONUtil.toBean(cacheValue, Page.class);
            return cachePage;
        }
        // 3. 都未命中，查询数据库
        Page<Picture> picturePage = this.page(new Page<>(pictureQueryRequest.getCurrent(), pictureQueryRequest.getPageSize()),
                this.getQueryWrapper(pictureQueryRequest));
        Page<PictureVO> pictureVOPage = this.getPictureVOPage(picturePage, request);
        // 4. 更新本地缓存和redis缓存
        // 将数据重新存入 redis，设置过期时间5-10分钟
        String cacheValueVo = JSONUtil.toJsonStr(pictureVOPage);
        // 设置过期时间
        int cacheExpireSeconds = 300 + RandomUtil.randomInt(0, 300); // 5-10分钟
        opsForValue.set(cacheKey, cacheValueVo, cacheExpireSeconds, TimeUnit.SECONDS);
        // 更新本地缓存
        LOCAL_CACHE.put(cacheKey, cacheValueVo);
        return pictureVOPage;
    }

    @Async
    @Override
    public void clearPictureFile(Picture oldPicture) {
        // 判断该图片是否被多条记录使用
        String pictureUrl = oldPicture.getUrl();
        long count = this.lambdaQuery()
                .eq(Picture::getUrl, pictureUrl)
                .count();
        // 有不止一条记录用到了该图片，不清理
        if (count > 1) {
            return;
        }
        // FIXME 注意，这里的 url 包含了域名，实际上只要传 key 值（存储路径）就够了
        cosManager.deleteObject(oldPicture.getUrl());
        // 清理缩略图
        String thumbnailUrl = oldPicture.getThumbnailUrl();
        if (StrUtil.isNotBlank(thumbnailUrl)) {
            cosManager.deleteObject(thumbnailUrl);
        }
    }

}




