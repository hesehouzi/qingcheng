package com.qingcheng.service.impl;
import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qingcheng.dao.AuditlogMapper;
import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.goods.Auditlog;
import com.qingcheng.service.goods.AuditlogService;
import org.springframework.beans.factory.annotation.Autowired;
import tk.mybatis.mapper.entity.Example;

import java.util.List;
import java.util.Map;

@Service
public class AuditlogServiceImpl implements AuditlogService {

    @Autowired
    private AuditlogMapper auditlogMapper;

    /**
     * 返回全部记录
     * @return
     */
    public List<Auditlog> findAll() {
        return auditlogMapper.selectAll();
    }

    /**
     * 分页查询
     * @param page 页码
     * @param size 每页记录数
     * @return 分页结果
     */
    public PageResult<Auditlog> findPage(int page, int size) {
        PageHelper.startPage(page,size);
        Page<Auditlog> auditlogs = (Page<Auditlog>) auditlogMapper.selectAll();
        return new PageResult<Auditlog>(auditlogs.getTotal(),auditlogs.getResult());
    }

    /**
     * 条件查询
     * @param searchMap 查询条件
     * @return
     */
    public List<Auditlog> findList(Map<String, Object> searchMap) {
        Example example = createExample(searchMap);
        return auditlogMapper.selectByExample(example);
    }

    /**
     * 分页+条件查询
     * @param searchMap
     * @param page
     * @param size
     * @return
     */
    public PageResult<Auditlog> findPage(Map<String, Object> searchMap, int page, int size) {
        PageHelper.startPage(page,size);
        Example example = createExample(searchMap);
        Page<Auditlog> auditlogs = (Page<Auditlog>) auditlogMapper.selectByExample(example);
        return new PageResult<Auditlog>(auditlogs.getTotal(),auditlogs.getResult());
    }

    /**
     * 根据Id查询
     * @param id
     * @return
     */
    public Auditlog findById(Integer id) {
        return auditlogMapper.selectByPrimaryKey(id);
    }

    /**
     * 新增
     * @param auditlog
     */
    public void add(Auditlog auditlog) {
        auditlogMapper.insert(auditlog);
    }

    /**
     * 修改
     * @param auditlog
     */
    public void update(Auditlog auditlog) {
        auditlogMapper.updateByPrimaryKeySelective(auditlog);
    }

    /**
     *  删除
     * @param id
     */
    public void delete(Integer id) {
        auditlogMapper.deleteByPrimaryKey(id);
    }

    /**
     * 构建查询条件
     * @param searchMap
     * @return
     */
    private Example createExample(Map<String, Object> searchMap){
        Example example=new Example(Auditlog.class);
        Example.Criteria criteria = example.createCriteria();
        if(searchMap!=null){
            // ad_person
            if(searchMap.get("adPerson")!=null && !"".equals(searchMap.get("adPerson"))){
                criteria.andLike("adPerson","%"+searchMap.get("adPerson")+"%");
            }
            // ad_result
            if(searchMap.get("adResult")!=null && !"".equals(searchMap.get("adResult"))){
                criteria.andLike("adResult","%"+searchMap.get("adResult")+"%");
            }
            // ad_spec
            if(searchMap.get("adSpec")!=null && !"".equals(searchMap.get("adSpec"))){
                criteria.andLike("adSpec","%"+searchMap.get("adSpec")+"%");
            }

            // id
            if(searchMap.get("id")!=null ){
                criteria.andEqualTo("id",searchMap.get("id"));
            }

        }
        return example;
    }

}
