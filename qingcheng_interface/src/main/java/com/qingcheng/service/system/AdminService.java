package com.qingcheng.service.system;
import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.system.Admin;
import com.qingcheng.pojo.system.AdminAndRole;
import com.qingcheng.pojo.system.AdminAndRoles;

import java.util.*;

/**
 * admin业务逻辑层
 */
public interface AdminService {


    public List<Admin> findAll();


    public PageResult<Admin> findPage(int page, int size);


    public List<Admin> findList(Map<String,Object> searchMap);


    public PageResult<Admin> findPage(Map<String,Object> searchMap,int page, int size);


    public AdminAndRoles findById(Integer id);

    public void add(AdminAndRoles adminAndRoles);


    public void update(AdminAndRoles adminAndRoles);


    public void delete(Integer id);

    public Admin findAdminByUsername(String username);

}
