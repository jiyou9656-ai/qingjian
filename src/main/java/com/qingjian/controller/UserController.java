package com.qingjian.controller;


import cn.hutool.core.bean.BeanUtil;
import com.qingjian.dto.LoginFormDTO;
import com.qingjian.dto.Result;
import com.qingjian.dto.UserDTO;
import com.qingjian.entity.User;
import com.qingjian.entity.UserInfo;
import com.qingjian.service.IUserInfoService;
import com.qingjian.service.IUserService;
import com.qingjian.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

/**
 * <p>
 * 前端控制器
 */
@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private IUserService userService;

    @Resource
    private IUserInfoService userInfoService;

    /**
     * 发送手机验证码
     */
    @PostMapping("code")
    public Result sendCode(@RequestParam("phone") String phone, HttpSession session) {
        // 发送短信验证码并保存验证码
        return userService.sendCode(phone, session);
    }

    /**
     * 登录功能
     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
     */
    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginForm, HttpSession session){
        // 实现登录功能
        return userService.login(loginForm, session);
    }

    /**
     * 登出功能
     * @return 无
     */
    @PostMapping("/logout")
    public Result logout(HttpServletRequest request){
        // 清除session中的token
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.removeAttribute("user");
            session.invalidate();
        }
        UserHolder.removeUser();
        return Result.ok("退出登录成功");
    }

    @GetMapping("/me")
    public Result me(){
        // 获取当前登录的用户并返回
        UserDTO user = UserHolder.getUser();
        return Result.ok(user);
    }

    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId){
        // 查询详情
        UserInfo info = userInfoService.getById(userId);
        if (info == null) {
            // 没有详情，应该是第一次查看详情
            return Result.ok();
        }
        info.setCreateTime(null);
        info.setUpdateTime(null);
        // 返回
        return Result.ok(info);
    }

    @GetMapping("/{id}")
    public Result queryUserById(@PathVariable("id") Long userId){
        // 查询详情
        User user = userService.getById(userId);
        if (user == null) {
            return Result.ok();
        }
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        // 返回
        return Result.ok(userDTO);
    }

    @PostMapping("/sign")
    public Result sign(){
        return userService.sign();
    }

    @GetMapping("/sign/count")
    public Result signCount(){
        return userService.signCount();
    }

    @PostMapping("/info")
    public Result updateUserInfo(@RequestBody UserInfo userInfo, @RequestParam(required = false) String nickName){
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.fail("请先登录");
        }
        userInfoService.updateUserInfo(user.getId(), userInfo, nickName);
        return Result.ok();
    }

    @PostMapping("/upload")
    public Result uploadAvatar(@RequestParam("file") MultipartFile file) {
        UserDTO user = UserHolder.getUser();
        if (user == null) {
            return Result.fail("请先登录");
        }

        if (file == null || file.isEmpty()) {
            return Result.fail("请选择要上传的图片");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        String fileName = UUID.randomUUID().toString() + extension;
        
        String uploadPath = "D:\\JavaProject\\qingjian\\qingjian-front\\imgs\\icons";
        File dir = new File(uploadPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        File dest = new File(uploadPath, fileName);
        try {
            file.transferTo(dest);
            String iconPath = "/imgs/icons/" + fileName;
            
            User userEntity = userService.getById(user.getId());
            userEntity.setIcon(iconPath);
            userService.updateById(userEntity);
            
            return Result.ok(iconPath);
        } catch (IOException e) {
            log.error("上传头像失败", e);
            return Result.fail("上传失败");
        }
    }

    @GetMapping("/list")
    public Result listUsers() {
        Long currentUserId = UserHolder.getUser().getId();
        java.util.List<User> users = userService.lambdaQuery()
                .ne(User::getId, currentUserId)
                .list();
        java.util.List<UserDTO> userDTOs = users.stream()
                .map(u -> BeanUtil.copyProperties(u, UserDTO.class))
                .collect(java.util.stream.Collectors.toList());
        return Result.ok(userDTOs);
    }

    /**
     * 搜索用户 - 支持昵称或ID搜索
     */
    @GetMapping("/search")
    public Result searchUsers(@RequestParam(required = false) String keyword) {
        if (cn.hutool.core.util.StrUtil.isBlank(keyword)) {
            return Result.ok(java.util.Collections.emptyList());
        }
        
        Long currentUserId = UserHolder.getUser().getId();
        java.util.List<User> users;
        
        // 尝试按ID搜索
        try {
            Long userId = Long.parseLong(keyword.trim());
            User user = userService.getById(userId);
            if (user != null && !user.getId().equals(currentUserId)) {
                users = java.util.Collections.singletonList(user);
                java.util.List<UserDTO> userDTOs = users.stream()
                        .map(u -> BeanUtil.copyProperties(u, UserDTO.class))
                        .collect(java.util.stream.Collectors.toList());
                return Result.ok(userDTOs);
            }
        } catch (NumberFormatException e) {
            // ID格式不对，按昵称搜索
        }
        
        // 按昵称搜索
        users = userService.lambdaQuery()
                .ne(User::getId, currentUserId)
                .like(User::getNickName, keyword.trim())
                .last("LIMIT 50")
                .list();
        
        java.util.List<UserDTO> userDTOs = users.stream()
                .map(u -> BeanUtil.copyProperties(u, UserDTO.class))
                .collect(java.util.stream.Collectors.toList());
        return Result.ok(userDTOs);
    }
}