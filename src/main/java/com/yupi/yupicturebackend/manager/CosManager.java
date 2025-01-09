package com.yupi.yupicturebackend.manager;

import cn.hutool.core.io.FileUtil;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.COSObject;
import com.qcloud.cos.model.GetObjectRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import com.yupi.yupicturebackend.config.CosClientConfig;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Component
public class CosManager {


    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;


    /**
     * 上传对象
     *
     * @param key  唯一键
     * @param file 文件
     */
    public PutObjectResult putObject(String key, File file) {
        // 创建上传 PutObjectRequest 对象
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                file);
        return cosClient.putObject(putObjectRequest);
    }


    /**
     * 获取对象
     *
     * @param key 唯一键
     * @return
     */
    public COSObject getObject(String key) {
        // 创建上传 PutObjectRequest 对象
        GetObjectRequest putObjectRequest = new GetObjectRequest(cosClientConfig.getBucket(), key);
        return cosClient.getObject(putObjectRequest);
    }

    /**
     * 上传并解析图片（图片处理）
     */
    public PutObjectResult putPictureObject(String key, File file) {
        // 创建上传 PutObjectRequest 对象
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key,
                file);
        // 1. 对图片进行处理（获取基本信息也被视为一种处理）
        PicOperations picOperations = new PicOperations();
        // 1 表示返回原图信息
        picOperations.setIsPicInfo(1);

        // 图片压缩处理
        List<PicOperations.Rule> rules = new ArrayList<>(); // 规则列表
        String webpKey = FileUtil.mainName(key) + ".webp";
        PicOperations.Rule compressRule = new PicOperations.Rule();
        // 设置压缩规则（webp）
        compressRule.setFileId(webpKey);
        compressRule.setBucket(cosClientConfig.getBucket());
        compressRule.setRule("imageMogr2/format/webp");
        rules.add(compressRule);

        // 2. 构造处理函数
        picOperations.setRules(rules);
        putObjectRequest.setPicOperations(picOperations);
        // 3. 上传对象
        return cosClient.putObject(putObjectRequest);
    }
}
