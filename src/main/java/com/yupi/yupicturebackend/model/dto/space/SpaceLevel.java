package com.yupi.yupicturebackend.model.dto.space;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 空间等级权益
 */
@Data
@AllArgsConstructor
public class SpaceLevel {

    /**
     * 等级值
     */
    private int value;

    /**
     * 中文
     */
    private String text;

    /**
     * 最大图片数
     */
    private long maxCount;

    /**
     * 最大空间大小
     */
    private long maxSize;
}
