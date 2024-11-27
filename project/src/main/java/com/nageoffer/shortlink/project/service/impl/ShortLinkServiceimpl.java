package com.nageoffer.shortlink.project.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.Week;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nageoffer.shortlink.project.common.convension.exception.ServiceException;
import com.nageoffer.shortlink.project.common.enums.ValiDateTypeEnum;
import com.nageoffer.shortlink.project.dao.entity.*;
import com.nageoffer.shortlink.project.dao.mapper.*;
import com.nageoffer.shortlink.project.dto.req.ShortLinkCreateReqDTO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkPageReqDTO;
import com.nageoffer.shortlink.project.dto.req.ShortLinkUpdateReqDTO;
import com.nageoffer.shortlink.project.dto.resp.ShortLinkCountQueryRespDTO;
import com.nageoffer.shortlink.project.dto.resp.ShortLinkCreateRespDTO;
import com.nageoffer.shortlink.project.dto.resp.ShortLinkPageRespDTO;
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

import static com.nageoffer.shortlink.project.common.constant.RedisKeyConstant.GOTO_SHORT_LINK_KEY;
import static com.nageoffer.shortlink.project.common.constant.RedisKeyConstant.LOCK_GOTO_SHORT_LINK_KEY;
import static com.nageoffer.shortlink.project.common.constant.ShortLinkConstant.AMAP_REMOTE_URL;

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

    @Value("${short-link.stats.local.amap-key}")
    private String statsLocalAmapKey;

    /*
    * 创建短链接
    * */
    @Override
    public ShortLinkCreateRespDTO createShortLink(ShortLinkCreateReqDTO requestParam) {
        String shortLinkSuffix = generateSuffix(requestParam);
        String fullShortUrl = new StringBuilder(requestParam.getDomain())
                                .append("/")
                                .append(shortLinkSuffix)
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
        ShortLinkGoToDO shortLinkGoToDO = ShortLinkGoToDO.builder()
                .fullShortUrl(fullShortUrl)
                .gid(requestParam.getGid())
                .build();
        try {
            baseMapper.insert(shortLinkDO);
            shortLinkGoToMapper.insert(shortLinkGoToDO);
        } catch (Exception ex) {
            LambdaQueryWrapper<LinkDO> queryWrapper = Wrappers.lambdaQuery(LinkDO.class)
                    .eq(LinkDO::getFullShortUrl, fullShortUrl);
            LinkDO hasShortLink = baseMapper.selectOne(queryWrapper);
            if (hasShortLink != null){
                // 数据库确实存在相同的短链接
                log.warn("短链接: " + fullShortUrl + " 已存在，请勿重复创建");
                throw new ServiceException("短链接重复生成");
            }
        }
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
    public IPage<ShortLinkPageRespDTO> pageShortLink(ShortLinkPageReqDTO requestParam) {
        LambdaQueryWrapper<LinkDO> queryWrapper = Wrappers.lambdaQuery(LinkDO.class)
                .eq(LinkDO::getGid, requestParam.getGid())
                .eq(LinkDO::getEnableStatus, 0)
                .eq(LinkDO::getDelFlag, 0)
                .orderByDesc(LinkDO::getCreateTime);
        IPage<LinkDO> resultPage = baseMapper.selectPage(requestParam, queryWrapper);
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
                .eq(LinkDO::getGid, requestParam.getGid())
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

        }

    }

    @SneakyThrows
    @Override
    public void restoreUrl(String shortUri, ServletRequest request, ServletResponse response) {
        String serverName = request.getServerName();
        String fullShortUrl = serverName + "/" + shortUri;
        String originalLink = stringRedisTemplate.opsForValue().get(String.format(GOTO_SHORT_LINK_KEY, fullShortUrl));
        if (StrUtil.isNotBlank(originalLink)){
            shortLinkStats(fullShortUrl, null, request, response);
            ((HttpServletResponse) response).sendRedirect(originalLink);
            return;
        }
        boolean contains = shortUriCreateRegisterCachePenetrationBloomFilter.contains(fullShortUrl);
        if (!contains){
            ((HttpServletResponse) response).sendRedirect("/page/notfound");
            return;
        }
        String gotoIsNullShortLink = stringRedisTemplate.opsForValue().get(String.format(GOTO_SHORT_LINK_KEY, fullShortUrl));
        if (StrUtil.isNotBlank(gotoIsNullShortLink)){
            ((HttpServletResponse) response).sendRedirect("/page/notfound");
            return;
        }
        RLock lock = redissonClient.getLock(String.format(LOCK_GOTO_SHORT_LINK_KEY, fullShortUrl));
        lock.lock();
        try{
            originalLink = stringRedisTemplate.opsForValue().get(String.format(GOTO_SHORT_LINK_KEY, fullShortUrl));
            if (StrUtil.isNotBlank(originalLink)){
                shortLinkStats(fullShortUrl, null, request, response);
                ((HttpServletResponse) response).sendRedirect(originalLink);
                return;
            }
            LambdaQueryWrapper<ShortLinkGoToDO> linkGoToQueryWrapper = Wrappers.lambdaQuery(ShortLinkGoToDO.class)
                    .eq(ShortLinkGoToDO::getFullShortUrl, fullShortUrl);
            ShortLinkGoToDO shortLinkGoToDO = shortLinkGoToMapper.selectOne(linkGoToQueryWrapper);
            if (shortLinkGoToDO == null){
                // 短链接不存在, 设置空值
                stringRedisTemplate.opsForValue().set(String.format(GOTO_SHORT_LINK_KEY, fullShortUrl), "-", 30, TimeUnit.MINUTES);
                ((HttpServletResponse) response).sendRedirect("/page/notfound");
                return;
            }
            LambdaQueryWrapper<LinkDO> queryWrapper = Wrappers.lambdaQuery(LinkDO.class)
                    .eq(LinkDO::getGid, shortLinkGoToDO.getGid())
                    .eq(LinkDO::getFullShortUrl, fullShortUrl)
                    .eq(LinkDO::getDelFlag, 0)
                    .eq(LinkDO::getEnableStatus, 0);
            LinkDO shortLinkDO = baseMapper.selectOne(queryWrapper);
            if (shortLinkDO == null && shortLinkDO.getValidDate().before(new Date())){
                // 短链接已过期, 设置空值
                stringRedisTemplate.opsForValue().set(String.format(GOTO_SHORT_LINK_KEY, fullShortUrl), "-", 30, TimeUnit.MINUTES);
                ((HttpServletResponse) response).sendRedirect("/page/notfound");
                return;
            }
            // 短链接存在, 储存OriginUrl至Redis中
            stringRedisTemplate.opsForValue().set(
                    String.format(GOTO_SHORT_LINK_KEY, fullShortUrl),
                    shortLinkDO.getOriginUrl(),
                    LinkUtil.getLinkCacheValidTime(shortLinkDO.getValidDate()), TimeUnit.MILLISECONDS);
            shortLinkStats(fullShortUrl, shortLinkDO.getGid(), request, response);
            ((HttpServletResponse) response).sendRedirect(shortLinkDO.getOriginUrl());
        } finally {
            lock.unlock();
        }
    }

    private void shortLinkStats(String fullShortUrl, String gid, ServletRequest request, ServletResponse response) {
        Cookie[] cookies = ((HttpServletRequest) request).getCookies();
        AtomicBoolean uvFirstFlag = new AtomicBoolean(true);
        try {
            AtomicReference<String> uv = new AtomicReference<>();
            Runnable addResponseCookieTask = (() -> {
                uv.set(UUID.fastUUID().toString());
                Cookie uvCookie = new Cookie("uv", uv.get());
                uvCookie.setMaxAge(60 * 60 * 24 * 30);
                uvCookie.setPath(StrUtil.sub(fullShortUrl, fullShortUrl.indexOf("/"), fullShortUrl.length()));
                ((HttpServletResponse)response).addCookie(uvCookie);
                stringRedisTemplate.opsForSet().add("short-link:stats:uv:" + fullShortUrl, uv.get());
            });

            if (ArrayUtil.isNotEmpty(cookies)){
                Arrays.stream(cookies).filter(cookie -> "uv".equals(cookie.getName()))
                        .findFirst()
                        .map(Cookie::getValue)
                        .ifPresentOrElse(each -> {
                            uv.set(each);
                            Long uvAdded = stringRedisTemplate.opsForSet().add("short-link:stats:uv:" + fullShortUrl, each);
                            uvFirstFlag.set(uvAdded != null && uvAdded > 0L);

                        }, addResponseCookieTask::run);
            } else {
                addResponseCookieTask.run();
            }
            String remoteAddr = LinkUtil.getIp(((HttpServletRequest) request));
            Long uipAdded = stringRedisTemplate.opsForSet().add("short-link:stats:uip:" + fullShortUrl, remoteAddr);
            boolean uipFirstFlag = uipAdded != null && uipAdded > 0L;
            if (StrUtil.isBlank(gid)){
                LambdaQueryWrapper<ShortLinkGoToDO> queryWrapper = Wrappers.lambdaQuery(ShortLinkGoToDO.class)
                        .eq(ShortLinkGoToDO::getFullShortUrl, fullShortUrl);
                ShortLinkGoToDO shortLinkGoToDO = shortLinkGoToMapper.selectOne(queryWrapper);
                gid = shortLinkGoToDO.getGid();
            }
            int hour = DateUtil.hour(new Date(), true);
            Week week = DateUtil.dayOfWeekEnum(new Date());
            int dayOfWeek = week.getIso8601Value();
            LinkAccessStatsDO linkAccessStatsDO = LinkAccessStatsDO.builder()
                    .pv(1)
                    .uv(uvFirstFlag.get() ? 1 : 0)
                    .uip(uipFirstFlag? 1 : 0)
                    .fullShortUrl(fullShortUrl)
                    .gid(gid)
                    .hour(hour)
                    .weekday(dayOfWeek)
                    .date(new Date())
                    .build();
            linkAccessStatsMapper.shortLinkStats(linkAccessStatsDO);
            Map<String, Object> localParamMap = new HashMap<>();
            localParamMap.put("key", statsLocalAmapKey);
            localParamMap.put("ip", remoteAddr);

            String localResultStr = HttpUtil.get(AMAP_REMOTE_URL, localParamMap);
            JSONObject localResultObj = JSON.parseObject(localResultStr);
            String infoCode = localResultObj.getString("infocode");
            LinkLocalStatsDO linkLocalStatsDO;
            if (StrUtil.isNotBlank(infoCode) && StrUtil.equals(infoCode, "10000")){
                String province = localResultObj.getString("province");
                boolean unknownFlag = StrUtil.equals(province, "[]");
                linkLocalStatsDO = LinkLocalStatsDO.builder()
                        .fullShortUrl(fullShortUrl)
                        .country("CHI")
                        .province(unknownFlag ? "unknown" : province)
                        .city(unknownFlag ? "unknown" : localResultObj.getString("city"))
                        .adcode(unknownFlag ? "unknown" : localResultObj.getString("adcode"))
                        .cnt(1)
                        .gid(gid)
                        .date(new Date())
                        .build();
                linkLocalStatsMapper.shortLinkLocalStats(linkLocalStatsDO);
                String os = LinkUtil.getOs((HttpServletRequest) request);
                LinkOsStatsDO linkOsStatsDO = LinkOsStatsDO.builder()
                        .fullShortUrl(fullShortUrl)
                        .gid(gid)
                        .os(os)
                        .cnt(1)
                        .date(new Date())
                        .build();
                linkOsStatsMapper.shortLinkOsStats(linkOsStatsDO);
                String browser = LinkUtil.getBrowser(((HttpServletRequest) request));
                LinkBrowserStatsDO linkBrowserStatsDO = LinkBrowserStatsDO.builder()
                        .browser(browser)
                        .cnt(1)
                        .gid(gid)
                        .fullShortUrl(fullShortUrl)
                        .date(new Date())
                        .build();
                linkBrowserStatsMapper.shortLinkBrowserStats(linkBrowserStatsDO);

                LinkAccessLogsDO linkAccessLogsDO = LinkAccessLogsDO.builder()
                        .user(uv.get())
                        .ip(remoteAddr)
                        .fullShortUrl(fullShortUrl)
                        .gid(gid)
                        .os(os)
                        .browser(browser)
                        .build();
                linkAccessLogsMapper.insert(linkAccessLogsDO);
            }
        } catch (Throwable e) {
            log.error("短链接统计功能异常", e);
        }


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
}
