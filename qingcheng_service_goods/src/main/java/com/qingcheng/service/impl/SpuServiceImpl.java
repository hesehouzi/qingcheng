package com.qingcheng.service.impl;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qingcheng.dao.*;
import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.goods.*;
import com.qingcheng.service.goods.SkuService;
import com.qingcheng.service.goods.SpuService;
import com.qingcheng.util.IdWorker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.util.*;

@Service(interfaceClass = SpuService.class)
public class SpuServiceImpl implements SpuService {

    @Autowired
    private SpuMapper spuMapper;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private BrandMapper brandMapper;

    @Autowired
    private CategoryBrandMapper categoryBrandMapper;

    @Autowired
    private AuditlogMapper auditlogMapper;

    @Autowired
    private SkuService skuService;

    /**
     * 返回全部记录
     * @return
     */
    public List<Spu> findAll() {
        return spuMapper.selectAll();
    }

    /**
     * 分页查询
     * @param page 页码
     * @param size 每页记录数
     * @return 分页结果
     */
    public PageResult<Spu> findPage(int page, int size) {
        PageHelper.startPage(page,size);
        Page<Spu> spus = (Page<Spu>) spuMapper.selectAll();
        return new PageResult<Spu>(spus.getTotal(),spus.getResult());
    }

    /**
     * 条件查询
     * @param searchMap 查询条件
     * @return
     */
    public List<Spu> findList(Map<String, Object> searchMap) {
        Example example = createExample(searchMap);
        return spuMapper.selectByExample(example);
    }

    /**
     * 分页+条件查询
     * @param searchMap
     * @param page
     * @param size
     * @return
     */
    public PageResult<Spu> findPage(Map<String, Object> searchMap, int page, int size) {
        PageHelper.startPage(page,size);
        Example example = createExample(searchMap);
        Page<Spu> spus = (Page<Spu>) spuMapper.selectByExample(example);
        return new PageResult<Spu>(spus.getTotal(),spus.getResult());
    }

    /**
     * 根据Id查询
     * @param id
     * @return
     */
    public Spu findById(String id) {
        return spuMapper.selectByPrimaryKey(id);
    }

    /**
     * 新增
     * @param spu
     */
    public void add(Spu spu) {
        spuMapper.insert(spu);
    }

    /**
     * 修改
     * @param spu
     */
    public void update(Spu spu) {
        spuMapper.updateByPrimaryKeySelective(spu);
    }

    /**
     *  逻辑删除
     * @param id
     */
    public void delete(String id) {
        //0.先删除缓存中的价格
        HashMap map = new HashMap();
        map.put("spuId",id);
        List<Sku> skuList = skuService.findList(map);
        for (Sku sku : skuList) {
            skuService.deletePriceFromRedis(sku.getId());
        }
        Spu spu = spuMapper.selectByPrimaryKey(id);//通过id查询到这个spu对象
        //1.设置spu的is_delete的值为1
        if (spu!=null){
            spu.setIsDelete("1");
            spuMapper.updateByPrimaryKeySelective(spu);
            //2.设置对应的sku的对象的status状态为3 表示逻辑删除
            Sku sku = skuMapper.selectByPrimaryKey(spu.getId());
            sku.setStatus("3");
            skuMapper.updateByPrimaryKeySelective(sku);
        }
    }

    /**
     * 保存商品
     * @param goods
     */
    @Transactional
    public void saveGoods(Goods goods) {
        //1.保存一个spu信息
        Spu spu = goods.getSpu();
        if (spu.getId()==null){//新增
            spu.setId(idWorker.nextId()+"");//id使用雪花算法生成
            spuMapper.insert(spu);
        }else {//修改
            //1.删除原来的sku列表
            Example example =new Example(Sku.class);
            Example.Criteria criteria = example.createCriteria();
            criteria.andEqualTo("spuId",spu.getId());
            skuMapper.deleteByExample(example);
            //2.执行spu的修改
            spuMapper.updateByPrimaryKeySelective(spu);
        }
        Date date = new Date();
        //通过spu的category3Id字段 查询得到一个category对象
        Category category = categoryMapper.selectByPrimaryKey(spu.getCategory3Id());
        //通过spu的brandId字段查询到一个brand对象
        Brand brand = brandMapper.selectByPrimaryKey(spu.getBrandId());
        //2.保存sku列表信息
        List<Sku> skuList = goods.getSkuList();
        for (Sku sku : skuList) {
            if (sku.getId()==null){//新增
                sku.setId(idWorker.nextId()+"");//id使用雪花算法生成
                sku.setCreateTime(date);//设置产生日期
            }
            sku.setSpuId(spu.getId());//spuId使用spu.getId()获得
            //不启用规格的sku处理
            if (sku.getSpec()==null || "".equals(sku.getSpec())){
                sku.setSpec("{}");
            }
            //拼接sku的名称 sku名称={"颜色":"红","机身内存":"6G"}
            //1.先获得sku的name
            String name = sku.getName();
            //2.获得sku的spec 并将其转换成map结构保存
            Map<String,String> specMap = JSON.parseObject(sku.getSpec(), Map.class);
            //3.遍历map的value值并和name拼接
            for (String value : specMap.values()) {
                name+=" "+value;
            }
            sku.setName(name);//设置sku名称
            sku.setUpdateTime(date);//设置更新日期
            sku.setCategoryId(spu.getCategory3Id());//设置该sku的分类id
            sku.setCategoryName(category.getName());//设置该sku的分类名称
            sku.setBrandName(brand.getName());//设置该sku的品牌名称
            sku.setCommentNum(0);//评论数
            sku.setSaleNum(0);//销量
            skuMapper.insert(sku);
            skuService.savePriceToRedisById(sku.getId(),sku.getPrice());//将每一个sku价格存储到缓存中
        }
        //建立品牌和分类的关联
        CategoryBrand categoryBrand = new CategoryBrand();
        categoryBrand.setCategoryId(spu.getCategory3Id());
        categoryBrand.setBrandId(spu.getBrandId());
        int count = categoryBrandMapper.selectCount(categoryBrand);
        if (count==0){
            categoryBrandMapper.insert(categoryBrand);
        }

    }

    /**
     * 根据id查询goods
     * @param id
     * @return
     */
    public Goods findGoodsById(String id) {
        //1.先查询spu
        Spu spu = spuMapper.selectByPrimaryKey(id);
        //2.查询sku 使用条件查询 条件为sku的spuId==id
        Example example = new Example(Sku.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("spuId",id);
        List<Sku> skuList = skuMapper.selectByExample(example);
        //3.封装为组合实体类
        Goods goods = new Goods();
        goods.setSpu(spu);
        goods.setSkuList(skuList);
        return goods;
    }

    /**
     * 商品审核
     * @param id
     * @param status
     * @param message
     */
    @Transactional
    public void audit(String id, String status, String message) {
        //1.修改状态 审核状态和上架状态
        Spu spu =new Spu();
        spu.setId(id);
        spu.setStatus(status);
        if ("1".equals(status)){
            spu.setIsMarketable("1");//如果审核状态为1，那么自动上架，即将isMarketable设置为1
        }
        spuMapper.updateByPrimaryKeySelective(spu);
        //2.记录商品审核记录
        Date date = new Date();
        Auditlog auditlog = new Auditlog();
        auditlog.setId(idWorker.nextId()+"");//使用雪花算法生成auditLog的id
        auditlog.setAdTime(date);
        auditlog.setAdPerson("当前审核人员");
        auditlog.setAdResult(status);
        auditlog.setAdSpec(message);
        auditlogMapper.insert(auditlog);
        //3.记录商品日志
    }

    /**
     * 下架
     * @param id
     */
    public void pull(String id) {
        //1.修改状态
        Spu spu = new Spu();
        spu.setId(id);
        spu.setStatus("0");
        spuMapper.updateByPrimaryKeySelective(spu);
        //2.记录商品日志
    }

    /**
     * 上架商品
     * @param id
     */
    public void put(String id) {
        //1.修改状态
        //首先查询到这个spu
        Spu spu = spuMapper.selectByPrimaryKey(id);
        if (!"1".equals(spu.getStatus())){
            throw new RuntimeException("此商品未通过审核");
        }
        spu.setStatus("1");
        spuMapper.updateByPrimaryKeySelective(spu);
        //2.记录日志
    }

    /**
     * 批量上架
     * @param ids
     * @return
     */
    public int putMany(String[] ids) {

        //1.修改状态
        Spu spu =new Spu();
        spu.setIsMarketable("1");

        Example example = new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andIn("id", Arrays.asList(ids));
        criteria.andEqualTo("isMarketable","0");//下架的
        criteria.andEqualTo("status","1");//得是审核通过的才能上架
        int i = spuMapper.updateByExampleSelective(spu, example);
        //2.添加商品日志

        return i;
    }

    /**
     * 还原商品
     * @param id
     */
    public void restore(String id) {
        Spu spu = spuMapper.selectByPrimaryKey(id);
        if (spu!=null){
            spu.setIsDelete("0");
            spuMapper.updateByPrimaryKeySelective(spu);
        }
    }

    /**
     * 物理删除
     * @param id
     */
    public void relaDelete(String id) {
        //1.首先通过id查询到该spu对象 判断是否存在且是否已经逻辑删除
        Spu spu = spuMapper.selectByPrimaryKey(id);
        if (spu==null){
            throw new RuntimeException("该商品不存在！！");
        }
        if ("0".equals(spu.getIsDelete())){
            throw new RuntimeException("该商品未逻辑删除！！");
        }
        //2.判断完成之后 再执行删除
        spuMapper.deleteByPrimaryKey(id);
    }

    /**
     * 构建查询条件
     * @param searchMap
     * @return
     */
    private Example createExample(Map<String, Object> searchMap){
        Example example=new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();
        if(searchMap!=null){
            // 主键
            if(searchMap.get("id")!=null && !"".equals(searchMap.get("id"))){
                criteria.andLike("id","%"+searchMap.get("id")+"%");
            }
            // 货号
            if(searchMap.get("sn")!=null && !"".equals(searchMap.get("sn"))){
                criteria.andLike("sn","%"+searchMap.get("sn")+"%");
            }
            // SPU名
            if(searchMap.get("name")!=null && !"".equals(searchMap.get("name"))){
                criteria.andLike("name","%"+searchMap.get("name")+"%");
            }
            // 副标题
            if(searchMap.get("caption")!=null && !"".equals(searchMap.get("caption"))){
                criteria.andLike("caption","%"+searchMap.get("caption")+"%");
            }
            // 图片
            if(searchMap.get("image")!=null && !"".equals(searchMap.get("image"))){
                criteria.andLike("image","%"+searchMap.get("image")+"%");
            }
            // 图片列表
            if(searchMap.get("images")!=null && !"".equals(searchMap.get("images"))){
                criteria.andLike("images","%"+searchMap.get("images")+"%");
            }
            // 售后服务
            if(searchMap.get("saleService")!=null && !"".equals(searchMap.get("saleService"))){
                criteria.andLike("saleService","%"+searchMap.get("saleService")+"%");
            }
            // 介绍
            if(searchMap.get("introduction")!=null && !"".equals(searchMap.get("introduction"))){
                criteria.andLike("introduction","%"+searchMap.get("introduction")+"%");
            }
            // 规格列表
            if(searchMap.get("specItems")!=null && !"".equals(searchMap.get("specItems"))){
                criteria.andLike("specItems","%"+searchMap.get("specItems")+"%");
            }
            // 参数列表
            if(searchMap.get("paraItems")!=null && !"".equals(searchMap.get("paraItems"))){
                criteria.andLike("paraItems","%"+searchMap.get("paraItems")+"%");
            }
            // 是否上架
            if(searchMap.get("isMarketable")!=null && !"".equals(searchMap.get("isMarketable"))){
                criteria.andLike("isMarketable","%"+searchMap.get("isMarketable")+"%");
            }
            // 是否启用规格
            if(searchMap.get("isEnableSpec")!=null && !"".equals(searchMap.get("isEnableSpec"))){
                criteria.andLike("isEnableSpec","%"+searchMap.get("isEnableSpec")+"%");
            }
            // 是否删除
            if(searchMap.get("isDelete")!=null && !"".equals(searchMap.get("isDelete"))){
                criteria.andLike("isDelete","%"+searchMap.get("isDelete")+"%");
            }
            // 审核状态
            if(searchMap.get("status")!=null && !"".equals(searchMap.get("status"))){
                criteria.andLike("status","%"+searchMap.get("status")+"%");
            }

            // 品牌ID
            if(searchMap.get("brandId")!=null ){
                criteria.andEqualTo("brandId",searchMap.get("brandId"));
            }
            // 一级分类
            if(searchMap.get("category1Id")!=null ){
                criteria.andEqualTo("category1Id",searchMap.get("category1Id"));
            }
            // 二级分类
            if(searchMap.get("category2Id")!=null ){
                criteria.andEqualTo("category2Id",searchMap.get("category2Id"));
            }
            // 三级分类
            if(searchMap.get("category3Id")!=null ){
                criteria.andEqualTo("category3Id",searchMap.get("category3Id"));
            }
            // 模板ID
            if(searchMap.get("templateId")!=null ){
                criteria.andEqualTo("templateId",searchMap.get("templateId"));
            }
            // 运费模板id
            if(searchMap.get("freightId")!=null ){
                criteria.andEqualTo("freightId",searchMap.get("freightId"));
            }
            // 销量
            if(searchMap.get("saleNum")!=null ){
                criteria.andEqualTo("saleNum",searchMap.get("saleNum"));
            }
            // 评论数
            if(searchMap.get("commentNum")!=null ){
                criteria.andEqualTo("commentNum",searchMap.get("commentNum"));
            }

        }
        return example;
    }

}
