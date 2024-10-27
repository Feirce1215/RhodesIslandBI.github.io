package com.rhodes.BI.model.dto.user;

import java.io.Serializable;
import lombok.Data;

/**
 * 用户更新个人信息请求
 *
 *   
 */
@Data
public class UserUpdateMyRequest implements Serializable {

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 电话号码
     */
    private String phone;

    /**
     * 电子邮箱
     */
    private String email;

    /**
     * 性别
     */
    private Integer gender;

    /**
     * 原密码
     */
    private String oldPwd;

    /**
     * 新密码
     */
    private String pwd;

    /**
     * 确认密码
     */
    private String checkPwd;

    private static final long serialVersionUID = 1L;
}