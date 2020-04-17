package com.qingcheng.pojo.system;

import lombok.Data;

import java.io.Serializable;
import java.util.List;


/**
 * 组合实体类 包含管理员实体和对应的角色集合
 */
@Data
public class AdminAndRoles implements Serializable {

    private Admin admin;//管理员实体类

    private List<Role> roleList;

}
