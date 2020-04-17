package com.qingcheng.service.impl;
import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qingcheng.dao.AdminAndRolesMapper;
import com.qingcheng.dao.AdminAndRoleMapper;
import com.qingcheng.dao.AdminMapper;
import com.qingcheng.dao.RoleMapper;
import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.system.Admin;
import com.qingcheng.pojo.system.AdminAndRole;
import com.qingcheng.pojo.system.AdminAndRoles;
import com.qingcheng.pojo.system.Role;
import com.qingcheng.service.system.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service(interfaceClass = AdminService.class)
public class AdminServiceImpl implements AdminService {

    @Autowired
    private AdminMapper adminMapper;

    @Autowired
    private AdminAndRoleMapper adminAndRoleMapper;

    @Autowired
    private AdminAndRolesMapper adminAndRolesMapper;

    @Autowired
    private RoleMapper roleMapper;

    /**
     * 返回全部记录
     * @return
     */
    public List<Admin> findAll() {
        return adminMapper.selectAll();
    }

    /**
     * 分页查询
     * @param page 页码
     * @param size 每页记录数
     * @return 分页结果
     */
    public PageResult<Admin> findPage(int page, int size) {
        PageHelper.startPage(page,size);
        Page<Admin> admins = (Page<Admin>) adminMapper.selectAll();
        return new PageResult<Admin>(admins.getTotal(),admins.getResult());
    }

    /**
     * 条件查询
     * @param searchMap 查询条件
     * @return
     */
    public List<Admin> findList(Map<String, Object> searchMap) {
        Example example = createExample(searchMap);
        return adminMapper.selectByExample(example);
    }

    /**
     * 分页+条件查询
     * @param searchMap
     * @param page
     * @param size
     * @return
     */
    public PageResult<Admin> findPage(Map<String, Object> searchMap, int page, int size) {
        PageHelper.startPage(page,size);
        Example example = createExample(searchMap);
        Page<Admin> admins = (Page<Admin>) adminMapper.selectByExample(example);
        return new PageResult<Admin>(admins.getTotal(),admins.getResult());
    }

    /**
     * 根据Id查询
     * @param id
     * @return
     */
    @Transactional
    public AdminAndRoles findById(Integer id) {
        //1.创建一个AdminAndRoles对象 用于封装
        AdminAndRoles adminAndRoles = new AdminAndRoles();
        //2.需要根据id查询出admin和roleList
        //2.1通过id在admin表中查询得到对应的admin
        Admin admin = adminMapper.selectByPrimaryKey(id);
        //2.需要得到id对应的roleId 一个id可以对应多个roleId 那么中间表对象可以得到多个
        List<AdminAndRole> adminAndRoleList = (List<AdminAndRole>) adminAndRoleMapper.selectByPrimaryKey(id);
        if (adminAndRoleList!=null && adminAndRoleList.size()>0){
            List<Role> roleList = new ArrayList<Role>();
            for (AdminAndRole adminAndRole : adminAndRoleList) {
                Integer roleId = adminAndRole.getRoleId();
                //通过roleId查询得到每一个role对象
                Role role = roleMapper.selectByPrimaryKey(roleId);
                roleList.add(role);
                adminAndRoles.setAdmin(admin);
                adminAndRoles.setRoleList(roleList);
            }
        }
        return adminAndRoles;
    }


    /**
     * 新增
     * @param adminAndRoles
     */
    @Transactional
    public void add(AdminAndRoles adminAndRoles) {
        //1.通过前端传递的组合实体类 获得admin实体 并保存
        Admin admin = new Admin();
        admin.setLoginName(adminAndRoles.getAdmin().getLoginName());
        admin.setPassword(adminAndRoles.getAdmin().getPassword());
        adminMapper.insertSelective(admin);
        //2.取出roleList 并将roleId保存到管理员和角色中间表中
        List<Role> roleList = adminAndRoles.getRoleList();
        //3.实例化一个role和admin中间表对象
        AdminAndRole adminAndRole = new AdminAndRole();
        if (roleList!=null && roleList.size()>0){
            for (Role role : roleList) {
                adminAndRole.setAdminId(admin.getId());
                adminAndRole.setRoleId(role.getId());
                adminAndRoleMapper.insertSelective(adminAndRole);//在role和admin中间表增加一条数据
            }
        }

    }

    /**
     * 修改
     * @param adminAndRoles
     */
    public void update(AdminAndRoles adminAndRoles) {
        //1.删除原来的中间表数据
        Admin admin = adminAndRoles.getAdmin();
        adminAndRoleMapper.deleteByPrimaryKey(admin.getId());
        //2.创建adminAndRole对象
        AdminAndRole adminAndRole = new AdminAndRole();
        adminAndRole.setAdminId(admin.getId());
        List<Role> roleList = adminAndRoles.getRoleList();
        if (roleList!=null && roleList.size()>0){
            for (Role role : roleList) {
                adminAndRole.setRoleId(role.getId());
                adminAndRoleMapper.insertSelective(adminAndRole);//更新到数据库
            }
        }


    }

    /**
     *  删除
     * @param id
     */
    public void delete(Integer id) {
        adminMapper.deleteByPrimaryKey(id);
    }

    /**
     * 根据用户名查询对象
     * @param username
     * @return
     */
    public Admin findAdminByUsername(String username) {
        Admin admin = new Admin();
        admin.setLoginName(username);
        return adminMapper.selectOne(admin);
    }

    /**
     * 构建查询条件
     * @param searchMap
     * @return
     */
    private Example createExample(Map<String, Object> searchMap){
        Example example=new Example(Admin.class);
        Example.Criteria criteria = example.createCriteria();
        if(searchMap!=null){
            // 用户名
            if(searchMap.get("loginName")!=null && !"".equals(searchMap.get("loginName"))){
                //criteria.andLike("loginName","%"+searchMap.get("loginName")+"%");
                criteria.andEqualTo("loginName",searchMap.get("loginName"));
            }
            // 密码
            if(searchMap.get("password")!=null && !"".equals(searchMap.get("password"))){
                criteria.andLike("password","%"+searchMap.get("password")+"%");
            }
            // 状态
            if(searchMap.get("status")!=null && !"".equals(searchMap.get("status"))){
                criteria.andEqualTo("status",searchMap.get("status"));
            }

            // id
            if(searchMap.get("id")!=null ){
                criteria.andEqualTo("id",searchMap.get("id"));
            }

        }
        return example;
    }

}
