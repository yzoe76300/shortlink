package com.nageoffer.shortlink.project.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum ValiDateTypeEnum {
    /*
    *  永久有效
    * */
    PERMANENT(0),
    /*
     *  自定義有效期
     * */
    CUSTOM(1);

    @Getter
    private final int type;
}
