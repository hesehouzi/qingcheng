package com.qingcheng.service.impl;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.qingcheng.pojo.goods.Category;
import com.qingcheng.pojo.goods.Sku;
import com.qingcheng.pojo.order.OrderItem;
import com.qingcheng.service.goods.CategoryService;
import com.qingcheng.service.goods.SkuService;
import com.qingcheng.service.order.CartService;
import com.qingcheng.service.order.PreferentialService;
import com.qingcheng.util.CacheKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private RedisTemplate redisTemplate;



    @Override
    public List<Map<String, Object>> findCartList(String username) {
        System.out.println("从redis中提取购物车"+username);
        List<Map<String,Object>> cartList = (List<Map<String,Object>>) redisTemplate.boundHashOps(CacheKey.CART_LIST).get(username);
        if (null==cartList){
            cartList=new ArrayList<>();
        }
        return cartList;
    }

    @Reference
    private SkuService skuService;

    @Reference
    private CategoryService categoryService;


    @Override
    public void addItem(String username, String skuId, Integer num) {
        //1.通过username获得购物车 并进行遍历
        List<Map<String, Object>> cartList = findCartList(username);
        boolean flag=false;//是否在购物车中存在
        //1.1如果在购物车中存在
        for (Map<String, Object> map : cartList) {
            OrderItem orderItem = (OrderItem) map.get("item");
            if (orderItem.getSkuId().equals(skuId)){//如果存在 即与skuId相等
                if (orderItem.getNum()<=0){
                    cartList.remove(map);
                    flag=true;
                    break;
                }
                int weight = orderItem.getWeight() / orderItem.getNum();//单个商品的数量 而且此时orderItem.getNum()值为正 不会出现异常

                //如果商品在购物车中存在 那么需要更改orderItem对象字段的数值 数量 金额 重量
                orderItem.setNum(orderItem.getNum()+num);//数量变更
                orderItem.setMoney(orderItem.getPrice()*orderItem.getNum());//金额变更
                orderItem.setWeight(weight*orderItem.getNum());//重量变更
                //数量变更后再次进行校验
                if (orderItem.getNum()<=0){
                    cartList.remove(map);
                }
                flag=true;//这时 flag为true 表明在购物车中存在该对象
                break;//跳出循环
            }
        }
        //1.2如果在购物车中不存在 则添加到缓存中 以username cartList键值对的方式存入 小key是username 大key是CacheKey.CART_LIST
        if (flag==false){

            //1.2.1构建一个orderItem
            OrderItem orderItem = new OrderItem();
            //1.2.2设置orderItem属性值
            //通过skuId查询得到sku对象 orderItem需要的属性可以从sku来
            Sku sku = skuService.findById(skuId);
            //对于查询得到的sku进行判断
            if (sku==null){
                throw new RuntimeException("该商品不存在");
            }
            if (num<0){
                throw new RuntimeException("商品数量不合法");
            }
            orderItem.setSkuId(skuId);
            orderItem.setImage(sku.getImage());
            orderItem.setName(sku.getName());
            orderItem.setSpuId(sku.getSpuId());
            orderItem.setNum(num);
            orderItem.setPrice(sku.getPrice());
            orderItem.setMoney(sku.getPrice()*num);//金额计算
            orderItem.setWeight(sku.getWeight()*num);//重量计算
            //1.2.3接下来分别设置categoryId1、categoryId2、categoryId3
            //先到缓存查询category3 若缓存没有再到数据库查询
            Category category3 = (Category) redisTemplate.boundHashOps(CacheKey.CATEGORY).get(sku.getCategoryId());
            if (category3==null){
                category3 = categoryService.findById(sku.getCategoryId());//通过id查询得到三级分类对象
                //数据库查询到数据之后将其存储到缓存中
                redisTemplate.boundHashOps(CacheKey.CATEGORY).put(sku.getCategoryId(),category3);
            }
            orderItem.setCategoryId3(category3.getId());//设置orderItem的三级分类id值
            orderItem.setCategoryId2(category3.getParentId());//设置orderItem的二级分类id值
            //先到缓存查询category2 若缓存没有再到数据库查询
            Category category2 = (Category) redisTemplate.boundHashOps(CacheKey.CATEGORY).get(category3.getParentId());
            if (category2==null){
                category2 = categoryService.findById(category3.getParentId());//通过数据中二级分类id值查询得到二级分类对象
                //将查询到的category2对象以键值对方式保存到缓存中
                redisTemplate.boundHashOps(CacheKey.CATEGORY).put(category3.getParentId(),category2);
            }
            orderItem.setCategoryId1(category2.getParentId());//通过二级分类对象 查询得到一级分类id 并设置

            //1.2.4由于存入cartList中的是map 再构建一个map 将orderItem存入cartList
            Map map = new HashMap();
            map.put("item",orderItem);
            map.put("checked",true);
            cartList.add(map);
        }
        //2.新增或修改的对象 存入到redis中
        redisTemplate.boundHashOps(CacheKey.CART_LIST).put(username,cartList);
    }

    @Override
    public boolean updateChecked(String username, String skuId, boolean checked) {
        //1.通过username获得购物车
        List<Map<String, Object>> cartList = findCartList(username);
        boolean isOk=false;
        //判断缓存中食肉含有已购商品
        for (Map<String, Object> map : cartList) {//遍历获得每一个购物车对象
            OrderItem orderItem = (OrderItem) map.get("item");//获得每一个orderItem对象
            if (orderItem.getSkuId().equals(skuId)){//将传递过来的skuId和每一个orderItem中的skuId字段匹配 如果相同那么更新map中的checked
                map.put("checked",checked);
                isOk=true;
                break;
            }
        }
            if (isOk){
                //将更新checked值之后的cartList存入缓存
                redisTemplate.boundHashOps(CacheKey.CART_LIST).put(username,cartList);//缓存更新
            }
        return isOk;
    }

    @Override
    public void deleteCheckedCart(String username) {
        //得到未选中的购物车列表 就相当于删除已选中的
        List<Map<String, Object>> cartList = findCartList(username).stream().filter(cart -> (boolean) cart.get("checked") == false)
                .collect(Collectors.toList());
        redisTemplate.boundHashOps(CacheKey.CART_LIST).put(username,cartList);
    }


    @Autowired
    private PreferentialService preferentialService;

    /**
     * 计算购物车的优惠金额
     * @param username 当前登录人
     * @return
     */
    @Override
    public int preferential(String username) {

        //1.获取选中的购物车
        List<OrderItem> orderItemList = findCartList(username).stream()
                .filter(cart -> (boolean) cart.get("checked") == true).map(cart -> (OrderItem) cart.get("item"))
                .collect(Collectors.toList());

        //2.按分类聚合统计每个分类的金额  以分类作为key  以金额作为value 从这个list流中拿出两个字段作为map的key 和value
        // 分类   金额
        //  1      120
        //  2      120
        Map<Integer, IntSummaryStatistics> cartMap = orderItemList.stream().collect(Collectors
                .groupingBy(OrderItem::getCategoryId3, Collectors.summarizingInt(OrderItem::getMoney)));

        //3.循环结果，统计每个分类的优惠金额，并累加
        int allPreMoney=0;//购物车总优惠金额
        for (Integer categoryId : cartMap.keySet()) {
            //获取品类的消费金额 这里得到的是所有该categoryId3 对应的商品的消费金额总额
            int money = (int) cartMap.get(categoryId).getSum();
            int preMoney = preferentialService.findPreMoneyByCategory(categoryId,money);//得到该categoryId3对应商品的优惠金额
            System.out.println("商品分类："+categoryId+"=="+"消费金额："+money+"=="+"优惠金额："+preMoney+"实付金额："+(money-preMoney));
            allPreMoney+=preMoney;
            System.out.println("购物车总优惠金额："+allPreMoney);
        }

        return allPreMoney;
    }

    /**
     * 获取最新的购物车列表
     * @param username 当前登录人
     * @return
     */
    @Override
    public List<Map<String, Object>> findNewOrderItemList(String username) {
        //1.获取购物车
        List<Map<String, Object>> cartList = findCartList(username);

        //2.循环购物车 刷新价格
        for (Map<String, Object> cart : cartList) {
            //2.1通过cart获得OrderItem对象
            OrderItem orderItem = (OrderItem) cart.get("item");
            //2.2通过orderItem获得skuId
            String skuId = orderItem.getSkuId();
            //2.3数据库查询skuId对应的sku对象 通过sku对象获得最新的price 然后更新购物车orderItem中的price和金额
            Sku sku = skuService.findById(skuId);
            orderItem.setPrice(sku.getPrice());//更新价格
            orderItem.setMoney(sku.getPrice()*orderItem.getNum());//更新金额
        }
        //3.保存最新购物车
        redisTemplate.boundHashOps(CacheKey.CART_LIST).put(username,cartList);

        return cartList;
    }
}
