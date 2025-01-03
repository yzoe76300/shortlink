package com.nageoffer.shortlink.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nageoffer.shortlink.project.common.convension.exception.ClientException;
import com.nageoffer.shortlink.project.common.convension.exception.ServiceException;
import com.nageoffer.shortlink.project.common.enums.ValiDateTypeEnum;
import com.nageoffer.shortlink.project.config.GotoDomainWhiteListConfiguration;
import com.nageoffer.shortlink.project.dao.entity.LinkDO;
import com.nageoffer.shortlink.project.dao.entity.ShortLinkGoToDO;
import com.nageoffer.shortlink.project.dao.mapper.*;
import com.nageoffer.shortlink.project.dto.biz.ShortLinkStatsRecordDTO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkBatchCreateReqDTO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkPageReqDTO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkUpdateReqDTO;
import com.nageoffer.shortlink.project.dto.resp.*;
import com.nageoffer.shortlink.project.mq.producer.ShortLinkStatsSaveProducer;
import com.nageoffer.shortlink.project.service.IShortLinkService;
import com.nageoffer.shortlink.project.toolkit.HashUtil;
import com.nageoffer.shortlink.project.toolkit.LinkUtil;
import groovy.util.logging.Slf4j;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static com.nageoffer.shortlink.project.common.constant.RedisKeyConstant.*;

/*
* 短链接接口实现层
* */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShortLinkServiceimpl extends ServiceImpl<linkMapper, LinkDO> implements IShortLinkService {

    private final RBloomFilter<String> shortUriCreateRegisterCachePenetrationBloomFilter ;
    private final linkMapper shortLinkMapper;
    private final ShortLinkGoToMapper shortLinkGoToMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    private final LinkAccessStatsMapper linkAccessStatsMapper;
    private final LinkLocalStatsMapper linkLocalStatsMapper;
    private final LinkOsStatsMapper linkOsStatsMapper;
    private final LinkBrowserStatsMapper linkBrowserStatsMapper;
    private final LinkAccessLogsMapper linkAccessLogsMapper;
    private final LinkDeviceStatsMapper linkDeviceStatsMapper;
    private final LinkNetworkStatsMapper linkNetworkStatsMapper;
    private final LinkStatsTodayMapper linkStatsTodayMapper;
    private final GotoDomainWhiteListConfiguration gotoDomainWhiteListConfiguration;
    private final ShortLinkStatsSaveProducer shortLinkStatsSaveProducer;
    /*
    *  默认高德key
    * */
    @Value("${short-link.stats.locale.amap-key}")
    private String statsLocalAmapKey;
    /*
    *  默认域名
    * */
    @Value(value = "${short-link.domain.default}")
    private String createShortLinkDefaultDomain;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO requestParam) {

        verificationWhitelist(requestParam.getOriginUrl());
        String shortLinkSuffix = generateSuffix(requestParam);
        String fullShortUrl = new StringBuilder(createShortLinkDefaultDomain)
                                .append("/")
                                .append(shortLinkSuffix)
                                .toString();
        LinkDO shortLinkDO = LinkDO.builder()
                        .domain(createShortLinkDefaultDomain)
                        .originUrl(requestParam.getOriginUrl())
                        .gid(requestParam.getGid())
                        .createdType(requestParam.getCreatedType())
                        .validDateType(requestParam.getValidDateType())
                        .validDate(requestParam.getValidDate())
                        .shortUri(shortLinkSuffix)
                        .describe(requestParam.getDescribe())
                        .enableStatus(0)
                        .totalPv(0)
                        .totalUv(0)
                        .totalUip(0)
                        .fullShortUrl(fullShortUrl)
                        .build();
        shortLinkDO.setFullShortUrl(fullShortUrl);
        shortLinkDO.setShortUri(shortLinkSuffix);
        shortLinkDO.setEnableStatus(0);
        ShortLinkGoToDO shortLinkGoToDO = ShortLinkGoToDO.builder()
                .fullShortUrl(fullShortUrl)
                .gid(requestParam.getGid())
                .build();
        try {
            baseMapper.insert(shortLinkDO);
            shortLinkGoToMapper.insert(shortLinkGoToDO);
        } catch (Exception ex) {
            // 数据库确实存在相同的短链接
            log.warn("短链接: " + fullShortUrl + " 已存在，请勿重复创建");
            throw new ServiceException(String.format("短链接: %s 已存在，请勿重复创建", fullShortUrl));
        }
        // 缓存预热，将短链接放入Redis缓存
        stringRedisTemplate.opsForValue().set(
                String.format(GOTO_SHORT_LINK_KEY, fullShortUrl),
                requestParam.getOriginUrl(),
                LinkUtil.getLinkCacheValidTime(requestParam.getValidDate()), TimeUnit.MILLISECONDS);
        shortUriCreateRegisterCachePenetrationBloomFilter.add(fullShortUrl);
        ShortLinkCreateRespDTO respDTO = ShortLinkCreateRespDTO.builder()
                .fullShortUrl("http://" + shortLinkDO.getFullShortUrl())
                .originUrl(requestParam.getOriginUrl())
                .gid(requestParam.getGid())
                .build();
        return respDTO;
    }

    @Override
    public ShortLinkBatchCreateRespDTO batchCreateShortLink(ShortLinkBatchCreateReqDTO requestParam) {
        List<String> originUrls = requestParam.getOriginUrls();
        List<String> describes = requestParam.getDescribes();
        List<ShortLinkBaseInfoRespDTO> result = new ArrayList<>();
        for (int i = 0; i < originUrls.size(); i++) {
            ShortLinkCreateReqDTO shortLinkCreateReqDTO = BeanUtil.toBean(requestParam, ShortLinkCreateReqDTO.class);
            shortLinkCreateReqDTO.setOriginUrl(originUrls.get(i));
            shortLinkCreateReqDTO.setDescribe(describes.get(i));
            try {
                ShortLinkCreateRespDTO shortLink = createShortLink(shortLinkCreateReqDTO);
                ShortLinkBaseInfoRespDTO linkBaseInfoRespDTO = ShortLinkBaseInfoRespDTO.builder()
                        .fullShortUrl(shortLink.getFullShortUrl())
                        .originUrl(shortLink.getOriginUrl())
                        .describe(describes.get(i))
                        .build();
                result.add(linkBaseInfoRespDTO);
            } catch (Throwable ex) {
                log.error("批量创建短链接失败，原始参数：{}" + originUrls.get(i));
            }
        }
        return ShortLinkBatchCreateRespDTO.builder()
                .total(result.size())
                .baseLinkInfos(result)
                .build();
    }
    @Override
    public IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkPageReqDTO requestParam) {
//        LambdaQueryWrapper<LinkDO> queryWrapper = Wrappers.lambdaQuery(LinkDO.class)
//                .eq(LinkDO::getGid, requestParam.getGid())
//                .eq(LinkDO::getEnableStatus, 0)
//                .eq(LinkDO::getDelFlag, 0)
//                .orderByDesc(LinkDO::getCreateTime);
        IPage<LinkDO> resultPage = baseMapper.pageLink(requestParam);
        return resultPage.convert(each ->
        {
            ShortLinkPageRespDTO result = BeanUtil.toBean(each, ShortLinkPageRespDTO.class);
            result.setDomain("http://" + result.getDomain());
            return result;
        });
    }

    @Override
    public List<ShortLinkCountQueryRespDTO> listGroupShortLink(List<String> requestParam) {
        QueryWrapper<LinkDO> queryWrapper = Wrappers.query(new LinkDO())
                .select("gid,count(*) as shortLinkCount")
                .in("gid", requestParam)
                .eq("enable_status", 0)
                .groupBy("gid");
        List<Map<String, Object>> linkDOList = shortLinkMapper.selectMaps(queryWrapper);
        return BeanUtil.copyToList(linkDOList, ShortLinkCountQueryRespDTO.class);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateShortLink(ShortLinkUpdateReqDTO requestParam) {
        LambdaQueryWrapper<LinkDO> queryWrapper = Wrappers.lambdaQuery(LinkDO.class)
                .eq(LinkDO::getGid, requestParam.getOriginGid())
                .eq(LinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                .eq(LinkDO::getDelFlag, 0)
                .eq(LinkDO::getEnableStatus, 0);
        LinkDO hasShortLinkDO = baseMapper.selectOne(queryWrapper);
        if (hasShortLinkDO == null){
            throw new ServiceException("短链接记录不存在");
        } else {
            LinkDO shortLinkDO = LinkDO.builder()
                    .domain(hasShortLinkDO.getDomain())
                    .shortUri(hasShortLinkDO.getShortUri())
                    .clickNum(hasShortLinkDO.getClickNum())
                    .favicon(hasShortLinkDO.getFavicon())
                    .createdType(hasShortLinkDO.getCreatedType())
                    .gid(requestParam.getGid())
                    .originUrl(requestParam.getOriginUrl())
                    .describe(requestParam.getDescribe())
                    .validDateType(requestParam.getValidDateType())
                    .validDate(requestParam.getValidDate())
                    .build();
            if (Objects.equals(requestParam.getGid(), hasShortLinkDO.getGid())){
                LambdaUpdateWrapper<LinkDO> updateWrapper = Wrappers.lambdaUpdate(LinkDO.class)
                        .eq(LinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(LinkDO::getGid, requestParam.getGid())
                        .eq(LinkDO::getDelFlag, 0)
                        .eq(LinkDO::getEnableStatus, 0)
                        .set(Objects.equals(requestParam.getValidDateType(), ValiDateTypeEnum.PERMANENT.getType()),
                                LinkDO::getValidDate, null);

                baseMapper.update(shortLinkDO, updateWrapper);
            } else {
                LambdaUpdateWrapper<LinkDO> updateWrapper = Wrappers.lambdaUpdate(LinkDO.class)
                        .eq(LinkDO::getFullShortUrl, requestParam.getFullShortUrl())
                        .eq(LinkDO::getGid, hasShortLinkDO.getGid())
                        .eq(LinkDO::getDelFlag, 0)
                        .eq(LinkDO::getEnableStatus, 0);
                baseMapper.delete(updateWrapper);
                shortLinkDO.setGid(requestParam.getGid());
                baseMapper.insert(shortLinkDO);
            }

            if (!Objects.equals(hasShortLinkDO.getValidDateType(), requestParam.getValidDateType())
                    || !Objects.equals(hasShortLinkDO.getValidDate(), requestParam.getValidDate())
                    || !Objects.equals(hasShortLinkDO.getOriginUrl(), requestParam.getOriginUrl())) {
                stringRedisTemplate.delete(String.format(GOTO_SHORT_LINK_KEY, requestParam.getFullShortUrl()));
                Date currentDate = new Date();
                if (hasShortLinkDO.getValidDate() != null && hasShortLinkDO.getValidDate().before(currentDate)) {
                    if (Objects.equals(requestParam.getValidDateType(), ValiDateTypeEnum.PERMANENT.getType()) || requestParam.getValidDate().after(currentDate)) {
                        stringRedisTemplate.delete(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, requestParam.getFullShortUrl()));
                    }
                }
            }
        }

    }

    @SneakyThrows
    @Override
    public void restoreUrl(String shortUri, ServletRequest request, ServletResponse response) {
        String serverName = request.getServerName();
        String serverPort = Optional.of(request.getServerPort())
                .filter(each -> !Objects.equals(each, 80))
                .filter(each -> !Objects.equals(each, 443))
                .map(String::valueOf)
                .map(each -> ":" + each)
                .orElse("");
        String fullShortUrl = serverName + serverPort + "/" + shortUri;
        String originalLink = stringRedisTemplate.opsForValue().get(String.format(GOTO_SHORT_LINK_KEY, fullShortUrl));
        if (StrUtil.isNotBlank(originalLink)){
            shortLinkStats(fullShortUrl, buildLinkStatsRecordAndSetUser(fullShortUrl, request, response));
            ((HttpServletResponse) response).sendRedirect(originalLink);
            return;
        }
        boolean contains = shortUriCreateRegisterCachePenetrationBloomFilter.contains(fullShortUrl);
        if (!contains){
            ((HttpServletResponse) response).sendRedirect("/page/notfound");
            return;
        }
        String gotoIsNullShortLink = stringRedisTemplate.opsForValue().get(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl));
        if (StrUtil.isNotBlank(gotoIsNullShortLink)){
            ((HttpServletResponse) response).sendRedirect("/page/notfound");
            return;
        }
        RLock lock = redissonClient.getLock(String.format(LOCK_GOTO_SHORT_LINK_KEY, fullShortUrl));
        lock.lock();
        try{
            originalLink = stringRedisTemplate.opsForValue().get(String.format(GOTO_SHORT_LINK_KEY, fullShortUrl));
            if (StrUtil.isNotBlank(originalLink)){
                shortLinkStats(fullShortUrl, buildLinkStatsRecordAndSetUser(fullShortUrl, request, response));
                ((HttpServletResponse) response).sendRedirect(originalLink);
                return;
            }
            gotoIsNullShortLink = stringRedisTemplate.opsForValue().get(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl));
            if (StrUtil.isNotBlank(gotoIsNullShortLink)) {
                ((HttpServletResponse) response).sendRedirect("/page/notfound");
                return;
            }
            LambdaQueryWrapper<ShortLinkGoToDO> linkGoToQueryWrapper = Wrappers.lambdaQuery(ShortLinkGoToDO.class)
                    .eq(ShortLinkGoToDO::getFullShortUrl, fullShortUrl);
            ShortLinkGoToDO shortLinkGoToDO = shortLinkGoToMapper.selectOne(linkGoToQueryWrapper);
            if (shortLinkGoToDO == null){
                // 短链接不存在, 设置空值
                stringRedisTemplate.opsForValue().set(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl), "-", 30, TimeUnit.MINUTES);
                ((HttpServletResponse) response).sendRedirect("/page/notfound");
                return;
            }
            LambdaQueryWrapper<LinkDO> queryWrapper = Wrappers.lambdaQuery(LinkDO.class)
                    .eq(LinkDO::getGid, shortLinkGoToDO.getGid())
                    .eq(LinkDO::getFullShortUrl, fullShortUrl)
                    .eq(LinkDO::getDelFlag, 0)
                    .eq(LinkDO::getEnableStatus, 0);
            LinkDO shortLinkDO = baseMapper.selectOne(queryWrapper);
            if (shortLinkDO == null && shortLinkDO.getValidDate() != null && shortLinkDO.getValidDate().before(new Date())){
                // 短链接已过期,设置空值
                stringRedisTemplate.opsForValue().set(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl), "-", 30, TimeUnit.MINUTES);
                ((HttpServletResponse) response).sendRedirect("/page/notfound");
                return;
            }
            // 短链接存在, 储存OriginUrl至Redis中
            stringRedisTemplate.opsForValue().set(
                    String.format(GOTO_SHORT_LINK_KEY, fullShortUrl),
                    shortLinkDO.getOriginUrl(),
                    LinkUtil.getLinkCacheValidTime(shortLinkDO.getValidDate()), TimeUnit.MILLISECONDS);
            shortLinkStats(fullShortUrl, buildLinkStatsRecordAndSetUser(fullShortUrl, request, response));
            ((HttpServletResponse) response).sendRedirect(shortLinkDO.getOriginUrl());
        } finally {
            lock.unlock();
        }
    }
    private ShortLinkStatsRecordDTO buildLinkStatsRecordAndSetUser(String fullShortUrl, ServletRequest request, ServletResponse response) {
        AtomicBoolean uvFirstFlag = new AtomicBoolean();
        Cookie[] cookies = ((HttpServletRequest) request).getCookies();
        AtomicReference<String> uv = new AtomicReference<>();
        Runnable addResponseCookieTask = () -> {
            uv.set(UUID.fastUUID().toString());
            Cookie uvCookie = new Cookie("uv", uv.get());
            uvCookie.setMaxAge(60 * 60 * 24 * 30);
            uvCookie.setPath(StrUtil.sub(fullShortUrl, fullShortUrl.indexOf("/"), fullShortUrl.length()));
            ((HttpServletResponse) response).addCookie(uvCookie);
            uvFirstFlag.set(Boolean.TRUE);
            stringRedisTemplate.opsForSet().add(SHORT_LINK_STATS_UV_KEY + fullShortUrl, uv.get());
        };
        if (ArrayUtil.isNotEmpty(cookies)) {
            Arrays.stream(cookies)
                    .filter(each -> Objects.equals(each.getName(), "uv"))
                    .findFirst()
                    .map(Cookie::getValue)
                    .ifPresentOrElse(each -> {
                        uv.set(each);
                        Long uvAdded = stringRedisTemplate.opsForSet().add(SHORT_LINK_STATS_UV_KEY + fullShortUrl, each);
                        uvFirstFlag.set(uvAdded != null && uvAdded > 0L);
                    }, addResponseCookieTask);
        } else {
            addResponseCookieTask.run();
        }
        String remoteAddr = LinkUtil.getIp(((HttpServletRequest) request));
        String os = LinkUtil.getOs(((HttpServletRequest) request));
        String browser = LinkUtil.getBrowser(((HttpServletRequest) request));
        String device = LinkUtil.getDevice(((HttpServletRequest) request));
        String network = LinkUtil.getNetwork(((HttpServletRequest) request));
        Long uipAdded = stringRedisTemplate.opsForSet().add(SHORT_LINK_STATS_UIP_KEY + fullShortUrl, remoteAddr);
        boolean uipFirstFlag = uipAdded != null && uipAdded > 0L;
        return ShortLinkStatsRecordDTO.builder()
                .fullShortUrl(fullShortUrl)
                .uv(uv.get())
                .uvFirstFlag(uvFirstFlag.get())
                .uipFirstFlag(uipFirstFlag)
                .remoteAddr(remoteAddr)
                .os(os)
                .browser(browser)
                .device(device)
                .network(network)
                .currentDate(new Date())
                .build();
    }

    private void shortLinkStats(String fullShortUrl, ShortLinkStatsRecordDTO statsRecord) {
        Map<String, String> produceMap = new HashMap<>();
        produceMap.put("fullShortUrl", fullShortUrl);
        produceMap.put("statsRecord", JSON.toJSONString(statsRecord));
        shortLinkStatsSaveProducer.send(produceMap);
//        Cookie[] cookies = ((HttpServletRequest) request).getCookies();
//        AtomicBoolean uvFirstFlag = new AtomicBoolean(true);
//        try {
//            AtomicReference<String> uv = new AtomicReference<>();
//            Runnable addResponseCookieTask = (() -> {
//                uv.set(UUID.fastUUID().toString());
//                Cookie uvCookie = new Cookie("uv", uv.get());
//                uvCookie.setMaxAge(60 * 60 * 24 * 30);
//                uvCookie.setPath(StrUtil.sub(fullShortUrl, fullShortUrl.indexOf("/"), fullShortUrl.length()));
//                ((HttpServletResponse)response).addCookie(uvCookie);
//                stringRedisTemplate.opsForSet().add(SHORT_LINK_STATS_UV_KEY + fullShortUrl, uv.get());
//            });
//
//            if (ArrayUtil.isNotEmpty(cookies)){
//                Arrays.stream(cookies).filter(cookie -> "uv".equals(cookie.getName()))
//                        .findFirst()
//                        .map(Cookie::getValue)
//                        .ifPresentOrElse(each -> {
//                            uv.set(each);
//                            Long uvAdded = stringRedisTemplate.opsForSet().add(SHORT_LINK_STATS_UV_KEY + fullShortUrl, each);
//                            uvFirstFlag.set(uvAdded != null && uvAdded > 0L);
//                        }, addResponseCookieTask::run);
//            } else {
//                addResponseCookieTask.run();
//            }
//            String remoteAddr = LinkUtil.getIp(((HttpServletRequest) request));
//            Long uipAdded = stringRedisTemplate.opsForSet().add(SHORT_LINK_STATS_UIP_KEY + fullShortUrl, remoteAddr);
//            boolean uipFirstFlag = uipAdded != null && uipAdded > 0L;
//            if (StrUtil.isBlank(gid)){
//                LambdaQueryWrapper<ShortLinkGoToDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkGoToDO.class)
//                        .eq(ShortLinkGoToDO::getFullShortUrl, fullShortUrl);
//                ShortLinkGoToDO shortLinkGoToDO = shortLinkGoToMapper.selectOne(queryWrapper);
//                gid = shortLinkGoToDO.getGid();
//            }
//            int hour = DateUtil.hour(new Date(), true);
//            Week week = DateUtil.dayOfWeekEnum(new Date());
//            int dayOfWeek = week.getIso8601Value();
//            LinkAccessStatsDO linkAccessStatsDO = LinkAccessStatsDO.builder()
//                    .pv(1)
//                    .uv(uvFirstFlag.get() ? 1 : 0)
//                    .uip(uipFirstFlag? 1 : 0)
//                    .fullShortUrl(fullShortUrl)
//                    .gid(gid)
//                    .hour(hour)
//                    .weekday(dayOfWeek)
//                    .date(new Date())
//                    .build();
//            linkAccessStatsMapper.shortLinkStats(linkAccessStatsDO);
//            Map<String, Object> localParamMap = new HashMap<>();
//            localParamMap.put("key", statsLocalAmapKey);
//            localParamMap.put("ip", remoteAddr);
//
//            String localResultStr = HttpUtil.get(AMAP_REMOTE_URL, localParamMap);
//            JSONObject localResultObj = JSON.parseObject(localResultStr);
//            String infoCode = localResultObj.getString("infocode");
//            LinkLocalStatsDO linkLocalStatsDO;
//            String actualProvince;
//            String actualCity;
//            if (StrUtil.isNotBlank(infoCode) && StrUtil.equals(infoCode, "10000")){
//                String province = localResultObj.getString("province");
//                boolean unknownFlag = StrUtil.equals(province, "[]");
//                linkLocalStatsDO = LinkLocalStatsDO.builder()
//                        .fullShortUrl(fullShortUrl)
//                        .country("CHI")
//                        .province(actualProvince = unknownFlag ? "unknown" : province)
//                        .city(actualCity = unknownFlag ? "unknown" : localResultObj.getString("city"))
//                        .adcode(unknownFlag ? "unknown" : localResultObj.getString("adcode"))
//                        .cnt(1)
//                        .gid(gid)
//                        .date(new Date())
//                        .build();
//                linkLocalStatsMapper.shortLinkLocalStats(linkLocalStatsDO);
//
//                String os = LinkUtil.getOs((HttpServletRequest) request);
//                LinkOsStatsDO linkOsStatsDO = LinkOsStatsDO.builder()
//                        .fullShortUrl(fullShortUrl)
//                        .gid(gid)
//                        .os(os)
//                        .cnt(1)
//                        .date(new Date())
//                        .build();
//                linkOsStatsMapper.shortLinkOsStats(linkOsStatsDO);
//
//                String browser = LinkUtil.getBrowser(((HttpServletRequest) request));
//                LinkBrowserStatsDO linkBrowserStatsDO = LinkBrowserStatsDO.builder()
//                        .browser(browser)
//                        .cnt(1)
//                        .gid(gid)
//                        .fullShortUrl(fullShortUrl)
//                        .date(new Date())
//                        .build();
//                linkBrowserStatsMapper.shortLinkBrowserStats(linkBrowserStatsDO);
//
//                String device = LinkUtil.getDevice(((HttpServletRequest) request));
//                LinkDeviceStatsDO linkDeviceStatsDO = LinkDeviceStatsDO.builder()
//                        .device(device)
//                        .cnt(1)
//                        .gid(gid)
//                        .fullShortUrl(fullShortUrl)
//                        .date(new Date())
//                        .build();
//                linkDeviceStatsMapper.shortLinkDeviceState(linkDeviceStatsDO);
//
//                String network = LinkUtil.getNetwork(((HttpServletRequest) request));
//                LinkNetworkStatsDO linkNetworkStatsDO = LinkNetworkStatsDO.builder()
//                        .network(network)
//                        .cnt(1)
//                        .gid(gid)
//                        .fullShortUrl(fullShortUrl)
//                        .date(new Date())
//                        .build();
//                linkNetworkStatsMapper.shortLinkNetworkState(linkNetworkStatsDO);
//
//                LinkAccessLogsDO linkAccessLogsDO = LinkAccessLogsDO.builder()
//                        .user(uv.get())
//                        .ip(remoteAddr)
//                        .fullShortUrl(fullShortUrl)
//                        .gid(gid)
//                        .os(os)
//                        .device(device)
//                        .locale("中国" + "-" + actualProvince + "-" + actualCity)
//                        .network(network)
//                        .browser(browser)
//                        .build();
//                linkAccessLogsMapper.insert(linkAccessLogsDO);
//
//                baseMapper.incrementStats(gid, fullShortUrl, 1, uvFirstFlag.get() ? 1 : 0, uipFirstFlag ? 1 : 0);
//
//                LinkStatsTodayDO linkStatsTodayDO = LinkStatsTodayDO.builder()
//                        .gid(gid)
//                        .fullShortUrl(fullShortUrl)
//                        .date(new Date())
//                        .todayPv(1)
//                        .todayUv(uvFirstFlag.get() ? 1 : 0)
//                        .todayUip(uipFirstFlag ? 1 : 0)
//                        .build();
//                linkStatsTodayMapper.shortLinkTodayState(linkStatsTodayDO);
//            }
//        } catch (Throwable e) {
//            log.error("短链接统计功能异常", e);
//        }


    }

    @Override
    public void shortLinkStats(ShortLinkStatsRecordDTO statsRecord) {
        Map<String, String> producerMap = new HashMap<>();
        producerMap.put("statsRecord", JSON.toJSONString(statsRecord));
        shortLinkStatsSaveProducer.send(producerMap);
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
            originUrl += UUID.randomUUID().toString();
            shortUri = HashUtil.hashToBase62(originUrl);
            if (!shortUriCreateRegisterCachePenetrationBloomFilter.contains(createShortLinkDefaultDomain+ "/" + shortUri)){
                break;
            }

            customGenerateCount++;
        }
        return shortUri;
    }


    /**
     * 获取网站的favicon图标链接
     * @param url 网站的URL
     * @return favicon图标链接，如果不存在则返回nulL
     */
    @SneakyThrows
    private String getFavicon(String url) {
        //创建URL对象
        URL targetUrl = new URL(url);
        //打开连接
        HttpURLConnection connection = (HttpURLConnection) targetUrl.openConnection();
        // 禁止自动处理重定向
        connection.setInstanceFollowRedirects(false);
        // 设置请求方法为GET
        connection.setRequestMethod("GET");
        //连接
        connection.connect();
        //获取响应码
        int responseCode = connection.getResponseCode();
        // 如果是重定向响应码
        if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
            //获取重定向的URL
            String redirectUrl = connection.getHeaderField("Location");
            //如果重定向URL不为空
            if (redirectUrl != null) {
                // 创建新的URL对象
                URL newUrl = new URL(redirectUrl);//打开新的连接
                connection = (HttpURLConnection) newUrl.openConnection();//设置请求方法为GET
                connection.setRequestMethod("GET");//连接
                connection.connect();//获取新的响应码
                responseCode = connection.getResponseCode();
            }
        }
        // 如果响应码为200(HTTP_OK)
        if (responseCode == HttpURLConnection.HTTP_OK) {
            Document document = Jsoup.connect(url).get();
            Element faviconLink = document.select("link[rel~=(?i)^(shortcut )?icon]").first();
            if (faviconLink != null) {
                return faviconLink.attr("abs:href");
            }
        }
        return null;
    }
    /**
     * 验证跳转链接是否在白名单中
     * @param originUrl 跳转链接
     */
    private void verificationWhitelist(String originUrl) {
        Boolean enable = gotoDomainWhiteListConfiguration.getEnable();
        // 是否开启白名单验证
        if (enable == null || !enable) {
            return;
        }
        String domain = LinkUtil.extractDomain(originUrl);
        if (StrUtil.isBlank(domain)) {
            throw new ClientException("跳转链接填写错误");
        }
        List<String> details = gotoDomainWhiteListConfiguration.getDetails();
        if (!details.contains(domain)) {
            throw new ClientException("演示环境为避免恶意攻击，请生成以下网站跳转链接：" + gotoDomainWhiteListConfiguration.getNames());
        }
    }
}
