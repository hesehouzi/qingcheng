package com.qingcheng.service.goods;
import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.goods.Auditlog;

import java.util.*;

/**
 * auditlog业务逻辑层
 */
public interface AuditlogService {


    public List<Auditlog> findAll();


    public PageResult<Auditlog> findPage(int page, int size);


    public List<Auditlog> findList(Map<String,Object> searchMap);


    public PageResult<Auditlog> findPage(Map<String,Object> searchMap,int page, int size);


    public Auditlog findById(Integer id);

    public void add(Auditlog auditlog);


    public void update(Auditlog auditlog);


    public void delete(Integer id);

}
