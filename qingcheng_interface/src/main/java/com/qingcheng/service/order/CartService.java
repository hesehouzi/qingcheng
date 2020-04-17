package com.qingcheng.service.order;

import java.util.List;
import java.util.Map;

public interface CartService {


    /**
     * 从redis中提取购物车
     * @param username
     * @return
     */
    public List<Map<String,Object>> findCartList(String username);

    /**
     * 添加商品到购物车
     * @param username 当前登录名
     * @param skuId 商品id
     * @param num 商品数量
     */
    public void addItem(String username,String skuId,Integer num);

    /**
     * 更新选中状态
     * @param username 当前登录名
     * @param skuId     商品id
     * @param checked   商品数量
     */
    public boolean updateChecked(String username,String skuId,boolean checked);

    /**
     * 删除选中的购物车
     * @param username
     */
    public void deleteCheckedCart(String username);

    /**
     * 计算购物车的优惠金额
     * @param username 当前登录人
     * @return
     */
    public int preferential(String username);

    /**
     * 获取最新的购物车列表
     * @param username 当前登录人
     * @return
     */
    public List<Map<String,Object>> findNewOrderItemList(String username);

}
