package com.yupi.yupicturebackend.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;


/**
 * 用户角色枚举
 */
@Getter
public enum UserRoleEnum {
    USER("用户","user"),

    ADMIN("管理员","ADMIN");

    private final String text;
    private final String value;

    UserRoleEnum(String text, String value){
        this.text = text;
        this.value = value;
    }

    /**
     * 根据value获取枚举
     * @param value
     * @return
     */
    public static UserRoleEnum getEnumByValue(String value){
        if(ObjUtil.isEmpty(value)) return null;
        for (UserRoleEnum item : UserRoleEnum.values()) {
            if(item.getValue().equals(value)){
                return item;
            }
        }
        return null;
    }

    // 如果有成千上万的枚举，优化策略：使用Map缓存，枚举初始化时填充到map中
    //    Map<String,UserRoleEnum> map = new HashMap<>();
    //    map.put("admin", ADMIN);

}
