package com.yupi.yupicturebackend.model.vo;



import lombok.Data;

import java.util.List;

@Data
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
