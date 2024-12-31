package com.nageoffer.shortlink.project.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nageoffer.shortlink.project.dao.entity.LinkDO;
import com.nageoffer.shortlink.project.dto.biz.ShortLinkStatsRecordDTO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkBatchCreateReqDTO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkPageReqDTO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkUpdateReqDTO;
import com.nageoffer.shortlink.project.dto.resp.ShortLinkBatchCreateRespDTO;
import com.nageoffer.shortlink.project.dto.resp.ShortLinkCountQueryRespDTO;
import com.nageoffer.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import com.nageoffer.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import java.util.List;

/**
 * 短链接服务接口
 * @author <NAME>
 *         Date: 2019-06-14
 */
public interface IShortLinkService extends IService<LinkDO> {
    /*
    *  创建短链接
    * */
    ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO requestParam);


    /**
     * 批量创建短链接
     *
     * @param requestParam 批量创建短链接请求参数
     * @return 批量创建短链接返回参数
     */
    ShortLinkBatchCreateRespDTO batchCreateShortLink(ShortLinkBatchCreateReqDTO requestParam);

    /**
     * 分页查询短链接
     * @param requestParam
     * @return
     */

    IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkPageReqDTO requestParam);
    /**
     * 查询短链分组数量
     * @param requestParam
     * @return
     */
    List<ShortLinkCountQueryRespDTO> listGroupShortLink(List<String> requestParam);
    /**
     * 短链更新
     * @param requestParam
     * @return
     */
    void updateShortLink(ShortLinkUpdateReqDTO requestParam);
    /**
     * 短链接跳转
     *
     */
    void restoreUrl(String shortUri, ServletRequest request, ServletResponse response);

    /**
     * 短链接统计
     *
     * @param statsRecord 短链接统计实体参数
     */
    void shortLinkStats(ShortLinkStatsRecordDTO statsRecord);
}
