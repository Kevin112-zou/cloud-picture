package com.yupi.yupicturebackend.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;

import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;

import com.yupi.yupicturebackend.config.CosClientConfig;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.exception.ThrowUtils;
import com.yupi.yupicturebackend.model.dto.file.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class FileManager {


    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;


    /**
     * 上传图片
     *
     * @param multipartFile    图片文件
     * @param uploadPathPrefix 上传路径前缀
     * @return 上传结果
     */
    public UploadPictureResult uploadPicture(MultipartFile multipartFile, String uploadPathPrefix) {
        // 校验文件
        validPicture(multipartFile);
        //图片上传地址
        String uuid = RandomUtil.randomString(16); // 随机生成文件名
        String originalFilename = multipartFile.getOriginalFilename(); // 获取原始文件名
        String date = DateUtil.formatDate(new Date()); // 使用时间戳作为文件名的一部分
        // 自己拼接文件上传路径，而不是使用原始的文件名称，可以增强安全性
        String uploadFilename = String.format("%s_%s.%s", date, uuid, FileUtil.getSuffix(originalFilename));
        // 最终上传的路径 = 路径前缀 + 文件名
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);
        // 解析结果并返回
        File file = null;
        try {
            // 上传文件到临时目录
            file = File.createTempFile(uploadPath, null); // 创建临时文件
            // 将文件写入到临时目录
            multipartFile.transferTo(file);
            // 上传文件到COS，必须添加图片处理参数，否则无法获取图片信息
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            // 获取图片对象信息
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            int picWidth = imageInfo.getWidth();
            int picHeight = imageInfo.getHeight();
            double picScale = NumberUtil.round((picWidth * 1.0) / picHeight, 2).doubleValue(); // 计算宽高比
            // 封装返回结果
            UploadPictureResult uploadPictureResult = new UploadPictureResult();
            uploadPictureResult.setUrl(cosClientConfig.getHost() + uploadPath); // 图片访问地址
            uploadPictureResult.setPicName(FileUtil.mainName(originalFilename)); // 图片名称
            uploadPictureResult.setPicSize(FileUtil.size(file)); // 图片大小
            uploadPictureResult.setPicWidth(picWidth); // 图片宽
            uploadPictureResult.setPicHeight(picHeight); // 图片高
            uploadPictureResult.setPicScale(picScale); // 宽高比
            uploadPictureResult.setPicFormat(imageInfo.getFormat()); // 图片格式

            // 返回文件路径
            return uploadPictureResult;
        } catch (Exception e) {
            // 上传失败
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            deleteTempFile(file);
        }

    }

    /**
     * 清理临时文件
     *
     * @param file 临时文件
     */

    public void deleteTempFile(File file) {
        // 删除临时文件
        if (file != null) {
            boolean delete = file.delete();
            if (!delete) {
                log.error("删除临时文件失败，filepath = {}", file.getAbsoluteFile());
            }
        }
    }

    /**
     * 校验图片
     *
     * @param multipartFile 图片文件
     */
    private void validPicture(MultipartFile multipartFile) {
        ThrowUtils.throwIf(multipartFile.isEmpty(), ErrorCode.PARAMS_ERROR, "文件不能为空");
        // 1, 校验文件大小
        ThrowUtils.throwIf(multipartFile.getSize() > 1024 * 1024 * 2, ErrorCode.PARAMS_ERROR, "文件大小不能超过2MB");
        // 2. 校验文件类型
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        // 允许上传的文件后缀列表
        final List<String> ALLOW_FILE_SUFFIX = Arrays.asList("png", "jpg", "jpeg", "gif", "webp");
        ThrowUtils.throwIf(!ALLOW_FILE_SUFFIX.contains(fileSuffix), ErrorCode.PARAMS_ERROR, "文件类型不允许");
    }
}
