package com.nageoffer.shortlink.admin.controller;

import cn.hutool.core.bean.BeanUtil;
import com.nageoffer.shortlink.admin.common.convension.result.Result;
import com.nageoffer.shortlink.admin.common.convension.result.Results;
import com.nageoffer.shortlink.admin.dto.req.UserLoginReqDTO;
import com.nageoffer.shortlink.admin.dto.req.UserRegisterReqDTO;
import com.nageoffer.shortlink.admin.dto.req.UserUpdateReqDTO;
import com.nageoffer.shortlink.admin.dto.resp.UserLoginRespDTO;
import com.nageoffer.shortlink.admin.dto.resp.UserRespDTO;
import com.nageoffer.shortlink.admin.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;

    /*
    * 根据用户名查询用户信息
    * */
    @GetMapping("/api/short-link/v1/user/{username}")
    public Result<UserRespDTO> getUserByUsername(@PathVariable("username") String username) {
        return Results.success(userService.getUserByUsername(username));
    }
    /*
     * 根据用户名查询真实用户信息
     * */
    @GetMapping("/api/short-link/v1/real/user/{username}")
    public Result<UserRespDTO> getRealUserByUsername(@PathVariable("username") String username) {
        return Results.success(BeanUtil.toBean(userService.getUserByUsername(username), UserRespDTO.class));
    }
    /*
     * 查询用户名是否存在
     * */
    @GetMapping("/api/short-link/v1/user/check")
    public Result<Boolean> CheckUserNames(@RequestParam("username") String username) {
        return Results.success(!userService.hasUsername(username));
    }

    /**
     * 注冊用戶
     * @param requestParam 注冊用戶請求參數
     */
    @PostMapping("/api/short-link/v1/user")
    public Result<Void> register(@RequestBody UserRegisterReqDTO requestParam){
        userService.register(requestParam);
        return Results.success();
    }

    /**
     * 更新用戶信息
     * @param updateParam
     * @return
     */
    @PutMapping("/api/short-link/v1/user")
    public Result<Void> update(@RequestBody UserUpdateReqDTO updateParam){
        userService.update(updateParam);
        return Results.success();
    }

    @PostMapping("/api/short-link/v1/user/login")
    public Result<UserLoginRespDTO> login(@RequestBody UserLoginReqDTO requestParam){
        UserLoginRespDTO result = userService.login(requestParam);
        return Results.success(result);
    }

    /**
     * 检查用户是否登录
     * @param uuid
     * @return
     */
    @GetMapping("/api/short-link/v1/user/check-login")
    public Result<Boolean> checkLogin(@RequestParam("uuid") String uuid, @RequestParam("username") String username) {
        return Results.success(userService.checkLogin(username,uuid));
    }
    /**
     * 注销用户
     * @param uuid
     * @return
     */
    @DeleteMapping("/api/short-link/v1/user/logout")
    public Result<Void> logout(@RequestParam("uuid") String uuid, @RequestParam("username") String username){
        // 登出功能: 1. 清除redis缓存中用户信息 2. 重定向到登录页面
        userService.logout(username, uuid);
        return Results.success();
    }
}
