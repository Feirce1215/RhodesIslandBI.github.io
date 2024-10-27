package com.rhodes.BI.controller;

import cn.hutool.core.io.FileUtil;
import com.rhodes.BI.common.BaseResponse;
import com.rhodes.BI.common.ErrorCode;
import com.rhodes.BI.common.ResultUtils;
import com.rhodes.BI.exception.BusinessException;
import com.rhodes.BI.exception.ThrowUtils;
import com.rhodes.BI.model.dto.user.UserUpdateMyRequest;
import com.rhodes.BI.model.entity.User;
import com.rhodes.BI.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/file")
@Slf4j
public class FileController {

    @Resource
    private UserService userService;

    @PostMapping("/avatar")
    public BaseResponse<Boolean> updateMyAvatar(@RequestPart("file") MultipartFile multipartFile,
                                              HttpServletRequest request) {
        // 校验文件
        long size = multipartFile.getSize();
        String originalFilename = multipartFile.getOriginalFilename();
        // 1.大小
        final long TEN_MB = 10 * 1024 * 1024L;
        ThrowUtils.throwIf(size > TEN_MB, ErrorCode.PARAMS_ERROR, "文件过大");
        // 2.后缀
        String suffix = FileUtil.getSuffix(originalFilename);
        final List<String> validFileSuffixList = Arrays.asList("jpg", "png", "bmp", "gif");
        ThrowUtils.throwIf(!validFileSuffixList.contains(suffix), ErrorCode.PARAMS_ERROR, "存在非法的文件后缀");

        // 获取当前项目的绝对磁盘路径
        String parent = request.getServletContext().getRealPath("upload");
        // 保存头像文件的文件夹
        File dir = new File(parent);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        // 保存的头像文件的文件名
        String filename = UUID.randomUUID() + "." + suffix;

        // 创建文件对象，表示保存的头像文件
        File dest = new File(dir, filename);
        // 执行保存头像文件
        try {
            multipartFile.transferTo(dest);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传文件时读写错误，请稍后重尝试");
        }

        // 头像路径
        String avatar = "/upload/" + filename;
        User loginUser = userService.getLoginUser(request);
        User user = new User();
        user.setUid(loginUser.getUid());
        user.setAvatar(avatar);

        // 将头像写入到数据库中
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);

        // 返回成功头像路径
        return ResultUtils.success(true);
    }
}
