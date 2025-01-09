package com.yupi.yupicturebackend.manager;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
@Deprecated
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
        ThrowUtils.throwIf(multipartFile.getSize() > 1024 * 1024 * 3, ErrorCode.PARAMS_ERROR, "文件大小不能超过3MB");
        // 2. 校验文件类型
        String fileSuffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        // 允许上传的文件后缀列表
        final List<String> ALLOW_FILE_SUFFIX = Arrays.asList("png", "jpg", "jpeg", "gif", "webp");
        ThrowUtils.throwIf(!ALLOW_FILE_SUFFIX.contains(fileSuffix), ErrorCode.PARAMS_ERROR, "文件类型不允许");
    }


    /*  此方法已被封装优化
    public UploadPictureResult uploadPictureByUrl(String fileUrl, String uploadPathPrefix) {
        // todo 校验文件
        // validPicture(multipartFile);
        validPicture(fileUrl);
        //图片上传地址
        String uuid = RandomUtil.randomString(16); // 随机生成文件名

        // todo: 获取文件名称
        String originalFilename = FileUtil.mainName(fileUrl);
        //String originalFilename = multipartFile.getOriginalFilename(); // 获取原始文件名

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
            // todo 将文件下载到临时目录
            HttpUtil.downloadFile(fileUrl, file);
            // multipartFile.transferTo(file);

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
     * 根据url校验图片
     *
     * @param fileUrl 图片地址
     */
    private void validPicture(String fileUrl) {
        // 1.校验非空
        ThrowUtils.throwIf(fileUrl == null || fileUrl.isEmpty(), ErrorCode.PARAMS_ERROR, "文件地址为空");
        // 2.校验 URL　格式
        try {
            new URL(fileUrl);
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件格式不正确");
        }
        // 3.校验　URL 协议(http,https)
        boolean res = fileUrl.startsWith("http://") || fileUrl.startsWith("https://");
        ThrowUtils.throwIf(!res, ErrorCode.PARAMS_ERROR, "仅支持http和https协议");

        // 4.校验 HEAD 请求是否存在
        HttpResponse httpResponse = null;
        try {
            httpResponse = HttpUtil.createRequest(Method.HEAD, fileUrl).execute();
            // 未正常返回，无需执行其他逻辑
            if (httpResponse.getStatus() != HttpStatus.HTTP_OK) {
                return;
            }

            // 5.校验图片大小
            String contentLengthStr = httpResponse.header("Content-Length");
            if (StrUtil.isNotBlank(contentLengthStr)) {
                try {
                    // 不为空才校验是否合法
                    long contentLength = NumberUtil.parseLong(contentLengthStr);
                    ThrowUtils.throwIf(contentLength > 1024 * 1024 * 3, ErrorCode.PARAMS_ERROR, "文件大小不能超过3MB");
                } catch (NumberFormatException e) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小不合法");
                }
            }
            // 6.校验图片类型
            String contentType = httpResponse.header("Content-Type");
            if (StrUtil.isNotBlank(contentType)) {
                // 不为空才校验是否合法
                final List<String> ALLOW_FILE_SUFFIX = Arrays.asList("image/png", "image/jpg", "image/jpeg", "image/gif", "image/webp");
                ThrowUtils.throwIf(!ALLOW_FILE_SUFFIX.contains(contentType), ErrorCode.PARAMS_ERROR, "文件类型不允许");
            }
        } finally {
            // 关闭连接,防止资源泄露
            if (httpResponse != null) {
                httpResponse.close();
            }
        }
    }
}
