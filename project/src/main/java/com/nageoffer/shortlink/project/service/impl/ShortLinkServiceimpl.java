package com.nageoffer.shortlink.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nageoffer.shortlink.project.common.convension.exception.ServiceException;
import com.nageoffer.shortlink.project.dao.entity.LinkDO;
import com.nageoffer.shortlink.project.dao.mapper.linkMapper;
import com.nageoffer.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkPageReqDTO;
import com.nageoffer.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import com.nageoffer.shortlink.project.dto.resp.ShortLinkPageRespDTO;
import com.nageoffer.shortlink.project.service.IShortLinkService;
import com.nageoffer.shortlink.project.toolkit.HashUtil;
import groovy.util.logging.Slf4j;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.springframework.stereotype.Service;

/*
* 短链接接口实现层
* */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShortLinkServiceimpl extends ServiceImpl<linkMapper, LinkDO> implements IShortLinkService {

    private final RBloomFilter<String> shortUriCreateRegisterCachePenetrationBloomFilter ;
    /*
    * 创建短链接
    * */
    @Override
    public ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO requestParam) {
        String shortLinkSuffix = generateSuffix(requestParam);
        String fullShortUrl = new StringBuilder(requestParam.getDomain())
                                .append("/")
                                .append(requestParam.getDomain())
                                .toString();
        LinkDO shortLinkDO = LinkDO.builder()
                        .domain(requestParam.getDomain())
                        .originUrl(requestParam.getOriginUrl())
                        .gid(requestParam.getGid())
                        .createdType(requestParam.getCreatedType())
                        .validDateType(requestParam.getValidDateType())
                        .validDate(requestParam.getValidDate())
                        .shortUri(shortLinkSuffix)
                        .describe(requestParam.getDescribe())
                        .enableStatus(0)
                        .fullShortUrl(fullShortUrl)
                        .build();
        shortLinkDO.setFullShortUrl(fullShortUrl);
        shortLinkDO.setShortUri(shortLinkSuffix);
        shortLinkDO.setEnableStatus(0);
        try {
            baseMapper.insert(shortLinkDO);
        } catch (Exception ex) {
            LambdaQueryWrapper<LinkDO> queryWrapper = Wrappers.lambdaQuery(LinkDO.class)
                    .eq(LinkDO::getFullShortUrl, fullShortUrl);
            LinkDO hasShortLink = baseMapper.selectOne(queryWrapper);
            if (hasShortLink != null){
                // 数据库确实存在相同的短链接
                log.warn("短链接: " + fullShortUrl + " 已存在，请勿重复创建");
                throw new ServiceException("短链接重复生成");
            }
            shortUriCreateRegisterCachePenetrationBloomFilter.add(shortLinkSuffix);
        }
        return ShortLinkCreateRespDTO.builder()
                .fullShortUrl(shortLinkDO.getFullShortUrl())
                .originUrl(requestParam.getOriginUrl())
                .gid(requestParam.getGid())
                .build();
    }

    @Override
    public IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkPageReqDTO requestParam) {
        LambdaQueryWrapper<LinkDO> queryWrapper = Wrappers.lambdaQuery(LinkDO.class)
                .eq(LinkDO::getGid, requestParam.getGid())
                .eq(LinkDO::getEnableStatus, 0)
                .eq(LinkDO::getDelFlag, 0)
                .orderByDesc(LinkDO::getCreateTime);
        IPage<LinkDO> resultPage = baseMapper.selectPage(requestParam, queryWrapper);
        return resultPage.convert(each -> BeanUtil.toBean(each, ShortLinkPageRespDTO.class));
    }

    /*
    * 生成短链接后缀
    * */
    private String generateSuffix(ShortLinkCreateReqDTO requestParam) {
        int customGenerateCount = 0;
        String shortUri;
        while(true){
            if (customGenerateCount > 10){
                throw new RuntimeException("短链接频繁生成，请稍后再试");
            }
            String originUrl = requestParam.getOriginUrl();
            // 使用毫秒数避免冲突
            originUrl += System.currentTimeMillis();
            shortUri = HashUtil.hashToBase62(originUrl);
            if (!shortUriCreateRegisterCachePenetrationBloomFilter.contains(requestParam.getDomain() + "/" + shortUri)){
                break;
            }
            LambdaQueryWrapper<LinkDO> queryWrapper = Wrappers.lambdaQuery(LinkDO.class)
                    .eq(LinkDO::getFullShortUrl, requestParam.getDomain() + "/" + shortUri);
            LinkDO linkDO = baseMapper.selectOne(queryWrapper);
            customGenerateCount++;
        }
        return shortUri;
    }
}
