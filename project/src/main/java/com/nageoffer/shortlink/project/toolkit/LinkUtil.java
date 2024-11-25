package com.nageoffer.shortlink.project.toolkit;

import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;

import java.util.Date;
import java.util.Optional;

import static com.nageoffer.shortlink.project.common.constant.ShortLinkConstant.DEFAULT_CACHE_VALID_TIME;

/*
*  短链接工具类
* */
public class LinkUtil {
    /*
    *  获取短链接有效期时间
    * */
    public static long getLinkCacheValidTime(Date ValidDate){
        return Optional.ofNullable(ValidDate).map(each -> DateUtil.between(new Date(), each, DateUnit.MS))
                .orElse(DEFAULT_CACHE_VALID_TIME);
    }
}