package com.nageoffer.shortlink.project.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nageoffer.shortlink.project.dao.entity.LinkDO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkPageReqDTO;
import com.nageoffer.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import com.nageoffer.shortlink.project.dto.resp.ShortLinkPageRespDTO;

/**
 * 短链接服务接口
 * @author <NAME>
 *         Date: 2019-06-14
 */
public interface IShortLinkService extends IService<LinkDO> {
    /*
    *  创建短链接请求参数
    * */
    ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO requestParam);

    /**
     *
     * @param requestParam
     * @return
     */

    IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkPageReqDTO requestParam);
}
