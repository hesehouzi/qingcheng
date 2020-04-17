package com.qingcheng.pojo.system;

import lombok.Data;

import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

/**
 * 管理员角色中间表实体类
 */
@Table(name = "tb_admin_role")
@Data
public class AdminAndRole implements Serializable {

    @Id
    private Integer adminId;//管理员Id

    @Id
    private Integer roleId;//角色Id

}

