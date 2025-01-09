package com.yupi.yupicturebackend.manager.upload;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.CIObject;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.qcloud.cos.model.ciModel.persistence.ProcessResults;
import com.yupi.yupicturebackend.config.CosClientConfig;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.manager.CosManager;
import com.yupi.yupicturebackend.model.dto.file.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

/**
 * 图片上传模板类
 */
@Slf4j
public abstract class PictureUploadTemplate {


    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private CosManager cosManager;


    /**
     * 上传图片
     *
     * @param inputSource      图片文件
     * @param uploadPathPrefix 上传路径前缀
     * @return 上传结果
     */
    public UploadPictureResult uploadPicture(Object inputSource, String uploadPathPrefix) {
        // 1.校验文件（本地/网络）
        validPicture(inputSource);
        // 2.图片上传地址
        String uuid = RandomUtil.randomString(16); // 随机生成文件名
        // 获取原始文件名(通用方法),可以是本地文件，也可以是网络图片
        String originalFilename = getOriginalFilename(inputSource); // 获取原始文件名
        String date = DateUtil.formatDate(new Date()); // 使用时间戳作为文件名的一部分
        // 自己拼接文件上传路径，而不是使用原始的文件名称，可以增强安全性
        String uploadFilename = String.format("%s_%s.%s", date, uuid, FileUtil.getSuffix(originalFilename));
        // 最终上传的路径 = 路径前缀 + 文件名
        String uploadPath = String.format("/%s/%s", uploadPathPrefix, uploadFilename);
        // 解析结果并返回
        File file = null;
        try {
            // 3.上传文件到临时目录
            file = File.createTempFile(uploadPath, null); // 创建临时文件
            // 处理文件来源（本地/网络）
            processFile(inputSource, file);
            // 4.上传文件到COS，必须添加图片处理参数，否则无法获取图片信息
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            // 获取图片对象信息
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            // 获取图片处理后的信息，封装返回结果
            ProcessResults processResults = putObjectResult.getCiUploadResult().getProcessResults();
            List<CIObject> objectList = processResults.getObjectList();
            if (CollUtil.isNotEmpty(objectList)) {
                // 获取压缩之后的图片信息
                CIObject compressdCiobject = objectList.get(0);
                // 封装压缩后的返回结果
                return buildResult(originalFilename, compressdCiobject);
            }
            // 封装返回结果
            return buildUploadPictureResult(imageInfo, uploadPath, originalFilename, file);
        } catch (Exception e) {
            // 上传失败
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            // 6.清理临时文件
            deleteTempFile(file);
        }

    }

    /**
     * 封装返回结果
     *
     * @param originalFilename  COS返回的图片信息
     * @param compressdCiobject 压缩后的图片信息
     * @return 上传结果封装类
     */

    private UploadPictureResult buildResult(String originalFilename, CIObject compressdCiobject) {
        int picWidth = compressdCiobject.getWidth();
        int picHeight = compressdCiobject.getHeight();
        double picScale = NumberUtil.round((picWidth * 1.0) / picHeight, 2).doubleValue(); // 计算宽高比
        // 封装返回结果
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + compressdCiobject.getKey()); // 图片访问地址
        uploadPictureResult.setPicName(FileUtil.mainName(originalFilename)); // 图片名称
        uploadPictureResult.setPicSize(compressdCiobject.getSize().longValue()); // 图片大小
        uploadPictureResult.setPicWidth(picWidth); // 图片宽
        uploadPictureResult.setPicHeight(picHeight); // 图片高
        uploadPictureResult.setPicScale(picScale); // 宽高比
        uploadPictureResult.setPicFormat(compressdCiobject.getFormat()); // 图片格式
        return uploadPictureResult;
    }

    /**
     * 封装返回结果
     *
     * @param imageInfo        COS返回的图片信息
     * @param uploadPath       上传路径
     * @param originalFilename 原始文件名
     * @param file             临时文件
     * @return 上传结果封装类
     */
    private UploadPictureResult buildUploadPictureResult(ImageInfo imageInfo, String uploadPath, String originalFilename, File file) {

        int picWidth = imageInfo.getWidth();
        int picHeight = imageInfo.getHeight();
        double picScale = NumberUtil.round((picWidth * 1.0) / picHeight, 2).doubleValue(); // 计算宽高比
        // 封装返回结果
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        uploadPictureResult.setUrl(cosClientConfig.getHost() + "/" + uploadPath); // 图片访问地址
        uploadPictureResult.setPicName(FileUtil.mainName(originalFilename)); // 图片名称
        uploadPictureResult.setPicSize(FileUtil.size(file)); // 图片大小
        uploadPictureResult.setPicWidth(picWidth); // 图片宽
        uploadPictureResult.setPicHeight(picHeight); // 图片高
        uploadPictureResult.setPicScale(picScale); // 宽高比
        uploadPictureResult.setPicFormat(imageInfo.getFormat()); // 图片格式
        return uploadPictureResult;
    }

    /**
     * 处理文件来源（本地/网络）
     *
     * @param inputSource 文件来源
     * @param file        临时文件
     */
    protected abstract void processFile(Object inputSource, File file) throws IOException;

    /**
     * 获取原始文件名（本地/网络）
     *
     * @param inputSource 文件来源
     * @return 原始文件名
     */
    protected abstract String getOriginalFilename(Object inputSource);

    /**
     * 校验输入源（本地/网络）
     *
     * @param inputSource 文件来源
     */
    protected abstract void validPicture(Object inputSource);

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


}
