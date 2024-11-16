package com.nageoffer.shortlink.admin.dto.req;

import lombok.Data;

/*
* 短链接分组修改参数
* */
@Data
public class ShortLinkGroupUpdateReqDTO {
    /*
    * 分组标识
    * */
    private String gid;
    /*
    * short link group name
    * */
    private String name;
}
