package com.yupi.yupicturebackend.model.vo;


import lombok.Data;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 图片标签分类列表视图
 */
@Data
@Component
public class PictureTagCategory {
    /**
     * 标签列表
     */
    private List<String> tags;
    /**
     * 分类列表
     */
    private List<String> categories;
}
