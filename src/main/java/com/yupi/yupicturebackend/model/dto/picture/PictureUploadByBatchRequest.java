package com.yupi.yupicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;


/**
 * 批量上传图片请求参数
 */
@Data
public class PictureUploadByBatchRequest implements Serializable {

    /**
     * 搜索关键词
     */
    private String searchText;

    /**
     * 抓取数量，默认为10
     */
    private Integer count = 10;

    /**
     * 图片名称前缀
     */
    private String namePrefix;

    /**
     * 图片标签
     */
    private List<String> tags;

    private static final long serialVersionUID = 1L;
}
