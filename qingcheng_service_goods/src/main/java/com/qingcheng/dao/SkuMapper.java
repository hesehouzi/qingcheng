package com.qingcheng.dao;

import com.qingcheng.pojo.goods.Sku;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import tk.mybatis.mapper.common.Mapper;

public interface SkuMapper extends Mapper<Sku> {

    /**
     * 扣减库存方法
     * @param skuId skuId
     * @param num 扣减数量
     */
    @Select("update tb_sku set num=num-#{num} where id=#{id}")
    public void deductionStock(@Param("num") Integer num,@Param("id") String skuId);

    /**
     * 添加销量
     * @param SkuId skuId
     * @param num 增加的数量
     */
    @Select("update tb_sku set sale_num=sale_num+#{num} where id=#{id}")
    public void addSaleNum(@Param("num") Integer num,@Param("id") String skuId);

}
