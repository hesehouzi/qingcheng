package com.qingcheng.service.impl;
import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qingcheng.dao.CategoryMapper;
import com.qingcheng.dao.SpecMapper;
import com.qingcheng.dao.TemplateMapper;
import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.goods.Category;
import com.qingcheng.pojo.goods.Spec;
import com.qingcheng.pojo.goods.Template;
import com.qingcheng.service.goods.SpecService;
import com.qingcheng.util.CacheKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.List;
import java.util.Map;

@Service(interfaceClass = SpecService.class)
public class SpecServiceImpl implements SpecService {

    @Autowired
    private SpecMapper specMapper;

    @Autowired
    private TemplateMapper templateMapper;

    /**
     * 返回全部记录
     * @return
     */
    public List<Spec> findAll() {
        return specMapper.selectAll();
    }

    /**
     * 分页查询
     * @param page 页码
     * @param size 每页记录数
     * @return 分页结果
     */
    public PageResult<Spec> findPage(int page, int size) {
        PageHelper.startPage(page,size);
        Page<Spec> specs = (Page<Spec>) specMapper.selectAll();
        return new PageResult<Spec>(specs.getTotal(),specs.getResult());
    }

    /**
     * 条件查询
     * @param searchMap 查询条件
     * @return
     */
    public List<Spec> findList(Map<String, Object> searchMap) {
        Example example = createExample(searchMap);
        return specMapper.selectByExample(example);
    }

    /**
     * 分页+条件查询
     * @param searchMap
     * @param page
     * @param size
     * @return
     */
    public PageResult<Spec> findPage(Map<String, Object> searchMap, int page, int size) {
        PageHelper.startPage(page,size);
        Example example = createExample(searchMap);
        Page<Spec> specs = (Page<Spec>) specMapper.selectByExample(example);
        return new PageResult<Spec>(specs.getTotal(),specs.getResult());
    }

    /**
     * 根据Id查询
     * @param id
     * @return
     */
    public Spec findById(Integer id) {
        return specMapper.selectByPrimaryKey(id);
    }

    /**
     * 新增
     * @param spec
     */
    @Transactional
    public void add(Spec spec) {
        specMapper.insert(spec);
        //1.通过spec对象中的templateId查询出template对象
        Template template = templateMapper.selectByPrimaryKey(spec.getTemplateId());
        //2.由于spec增加1,那么template的数量+1
        template.setSpecNum(template.getSpecNum()+1);
        //3.将+1后的数量更新到数据库
        templateMapper.updateByPrimaryKey(template);
    }

    /**
     * 修改
     * @param spec
     */
    public void update(Spec spec) {
        specMapper.updateByPrimaryKeySelective(spec);
    }

    /**
     *  删除
     * @param id
     */
    @Transactional
    public void delete(Integer id) {
        //0.通过id查询spec对象
        Spec spec = specMapper.selectByPrimaryKey(id);
        //1.通过spec对象中的templateId查询出template对象
        Template template = templateMapper.selectByPrimaryKey(spec.getTemplateId());
        //2.由于spec增加-,那么template的数量-1
        template.setSpecNum(template.getSpecNum()-1);
        //3.将-1后的数量更新到数据库
        templateMapper.updateByPrimaryKey(template);
        specMapper.deleteByPrimaryKey(id);
    }

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private CategoryMapper categoryMapper;

    /**
     * 将规格列表保存到缓存
     */
    public void saveAllSpecToRedis() {
            //1.查询得到所有的category
            List<Category> categoryList = categoryMapper.selectAll();
            for (Category category : categoryList) {//遍历得到每一个category 这里是需要每一个category对象的templateID字段
                //2.通过specMapper查询得到specList集合
                List<Map> specList = specMapper.findListByCategoryName(category.getName());
                redisTemplate.boundHashOps(CacheKey.SPEC).put(category.getName(),specList);
            }
    }

    /**
     * 通过categoryName查询specList
     * @param categoryName
     * @return
     */
    public List<Map> findSpecFromRedisByCategoryName(String categoryName) {
        List<Map> specList = (List<Map>) redisTemplate.boundHashOps(CacheKey.SPEC).get(categoryName);
        return specList;
    }

    /**
     * 构建查询条件
     * @param searchMap
     * @return
     */
    private Example createExample(Map<String, Object> searchMap){
        Example example=new Example(Spec.class);
        Example.Criteria criteria = example.createCriteria();
        if(searchMap!=null){
            // 名称
            if(searchMap.get("name")!=null && !"".equals(searchMap.get("name"))){
                criteria.andLike("name","%"+searchMap.get("name")+"%");
            }
            // 规格选项
            if(searchMap.get("options")!=null && !"".equals(searchMap.get("options"))){
                criteria.andLike("options","%"+searchMap.get("options")+"%");
            }

            // ID
            if(searchMap.get("id")!=null ){
                criteria.andEqualTo("id",searchMap.get("id"));
            }
            // 排序
            if(searchMap.get("seq")!=null ){
                criteria.andEqualTo("seq",searchMap.get("seq"));
            }
            // 模板ID
            if(searchMap.get("templateId")!=null ){
                criteria.andEqualTo("templateId",searchMap.get("templateId"));
            }

        }
        return example;
    }

}
