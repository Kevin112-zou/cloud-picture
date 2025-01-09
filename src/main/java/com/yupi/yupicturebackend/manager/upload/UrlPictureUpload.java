package com.yupi.yupicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.yupi.yupicturebackend.exception.BusinessException;
import com.yupi.yupicturebackend.exception.ErrorCode;
import com.yupi.yupicturebackend.exception.ThrowUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

/**
 * url上传图片的实现类
 */
@Service
public class UrlPictureUpload extends PictureUploadTemplate {
    @Override
    protected void processFile(Object inputSource, File file) throws IOException {
        String fileUrl = (String) inputSource;
        HttpUtil.downloadFile(fileUrl, file);
    }

    @Override
    protected String getOriginalFilename(Object inputSource) {
        String fileUrl = (String) inputSource;
        return FileUtil.mainName(fileUrl);
    }

    @Override
    protected void validPicture(Object inputSource) {
        String fileUrl = (String) inputSource;
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
        ThrowUtils.throwIf(!res, ErrorCode.PARAMS_ERROR, "仅支持 HTTP 和 HTTPS　协议");

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
