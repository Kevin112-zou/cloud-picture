package com.yupi.yupicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;


/**
 * 图片上传请求
 */
@Data
public class PictureUploadRequest implements Serializable {

    /**
     * 图片id(用户修改)
     */
    private Long id;

    /**
     * 文件地址
     */
    private String fileUrl;

    /**
     * 指定图片名称
     */
    private String picName;

    /**
     * 图片标签list
     */
    private List<String> tags;

    /**
     * 图片空间id
     */
    private Long spaceId;

    private static final long serialVersionUID = 1L;
}
