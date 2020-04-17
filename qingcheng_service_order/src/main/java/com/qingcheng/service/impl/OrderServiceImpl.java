package com.qingcheng.service.impl;
import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qingcheng.dao.OrderConfigMapper;
import com.qingcheng.dao.OrderItemMapper;
import com.qingcheng.dao.OrderLogMapper;
import com.qingcheng.dao.OrderMapper;
import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.goods.Sku;
import com.qingcheng.pojo.order.*;
import com.qingcheng.service.goods.SkuService;
import com.qingcheng.service.goods.StockBackService;
import com.qingcheng.service.order.CartService;
import com.qingcheng.service.order.OrderItemService;
import com.qingcheng.service.order.OrderService;
import com.qingcheng.service.pay.WeixinPayService;
import com.qingcheng.util.IdWorker;
import org.aspectj.weaver.ast.Var;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import tk.mybatis.mapper.entity.Example;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service(timeout = 5000,interfaceClass = OrderService.class)
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper orderItemMapper;

    @Autowired
    private OrderLogMapper orderLogMapper;

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private OrderConfigMapper orderConfigMapper;

    /**
     * 返回全部记录
     * @return
     */
    public List<Order> findAll() {
        return orderMapper.selectAll();
    }

    /**
     * 分页查询
     * @param page 页码
     * @param size 每页记录数
     * @return 分页结果
     */
    public PageResult<Order> findPage(int page, int size) {
        PageHelper.startPage(page,size);
        Page<Order> orders = (Page<Order>) orderMapper.selectAll();
        return new PageResult<Order>(orders.getTotal(),orders.getResult());
    }

    /**
     * 条件查询
     * @param searchMap 查询条件
     * @return
     */
    public List<Order> findList(Map<String, Object> searchMap) {
        Example example = createExample(searchMap);
        return orderMapper.selectByExample(example);
    }

    /**
     * 分页+条件查询
     * @param searchMap
     * @param page
     * @param size
     * @return
     */
    public PageResult<Order> findPage(Map<String, Object> searchMap, int page, int size) {
        PageHelper.startPage(page,size);
        Example example = createExample(searchMap);
        Page<Order> orders = (Page<Order>) orderMapper.selectByExample(example);
        return new PageResult<Order>(orders.getTotal(),orders.getResult());
    }

    /**
     * 根据Id查询
     * @param id
     * @return
     */
    public Order findById(String id) {
        return orderMapper.selectByPrimaryKey(id);
    }


    @Autowired
    private CartService cartService;

    @Reference
    private SkuService skuService;

    @Autowired
    private RabbitTemplate rabbitTemplate;



    /**
     * 新增订单
     * @param order
     */
    public Map<String,Object> add(Order order) {
        //1.获取选中的购物车
        List<Map<String, Object>> cartList = cartService.findNewOrderItemList(order.getUsername());//通过当前登录名查询当前用户对应的购物车对象
        List<OrderItem> orderItemList = cartList.stream().filter(cart -> (boolean) cart.get("checked"))
                .map(cart -> (OrderItem) cart.get("item")).collect(Collectors.toList());//对购物车list进行流操作 筛选得到订单列表

        //2.扣减库存
        if (!skuService.deductionStock(orderItemList)){
            throw new RuntimeException("库存扣减失败！！！");
        }

        try {
            //3.保存订单主表
            order.setId(idWorker.nextId()+"");//设置id
            IntStream numStream = orderItemList.stream().mapToInt(OrderItem::getNum);
            int totalNum = numStream.sum();
            order.setTotalNum(totalNum);//设置总数量
            IntStream moneyStream = orderItemList.stream().mapToInt(OrderItem::getMoney);
            int totalMoney = moneyStream.sum();
            order.setTotalMoney(totalMoney);//设置总金额
            int preMoney = cartService.preferential(order.getUsername());
            order.setPreMoney(preMoney);//设置优惠金额
            order.setPayMoney(totalMoney-preMoney);//设置支付金额
            order.setCreateTime(new Date());//订单创建时间
            order.setOrderStatus("0");//订单状态
            order.setConsignStatus("0");//发货状态
            order.setPayStatus("0");//支付状态

            orderMapper.insert(order);
            //4.保存订单明细表
            //打折比例
            double proportion = (double)order.getPayMoney() / totalMoney;
            for (OrderItem orderItem : orderItemList) {
                orderItem.setId(idWorker.nextId()+"");
                orderItem.setOrderId(order.getId());
                orderItem.setPayMoney((int) (orderItem.getMoney()*proportion));
                orderItemMapper.insert(orderItem);
            }
            //int x=1/0;
        } catch (Exception e) {
            e.printStackTrace();
            //发生异常 就将orderItemList将消息传递给mq 传递的内容是orderItemList
            rabbitTemplate.convertAndSend("","queue.skuback", JSON.toJSONString(orderItemList));
            throw new RuntimeException("创建订单失败");
        }

        //5.清除购物车
        cartService.deleteCheckedCart(order.getUsername());
        //6.发送消息到queue.order 发送orderId到queue.order
        rabbitTemplate.convertAndSend("","queue.order",order.getId());
        Map map = new HashMap();
        map.put("ordersn",order.getId());
        map.put("money",order.getPayMoney());
        return map;
    }

    /**
     * 修改
     * @param order
     */
    public void update(Order order) {
        orderMapper.updateByPrimaryKeySelective(order);
    }

    /**
     *  删除
     * @param id
     */
    public void delete(String id) {
        orderMapper.deleteByPrimaryKey(id);
    }

    /**
     * 查询OrderDetail组合实体类
     * @param orderId
     * @return
     */
    public OrderDetail getOrderDetailById(String orderId) {
        //1.通过orderId查询order实例
        Order order = orderMapper.selectByPrimaryKey(orderId);
        //2.条件查询到orderItem中orderId字段相对应的orderItem
        //2.1构建查询条件
        Example example = new Example(OrderItem.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("orderId",orderId);
        List<OrderItem> orderItemList = orderItemMapper.selectByExample(example);
        //3.对查询出来的order和orderItem进行封装
        OrderDetail orderDetail = new OrderDetail();
        orderDetail.setOrder(order);
        orderDetail.setOrderItemList(orderItemList);
        //4.返回orderDetail实例
        return orderDetail;
    }

    /**
     * 筛选查询
     * @param ids
     * @return
     */
    public List<Order> queryOrderStatus(String[] ids) {
        //1.构建order实例中orderStatus值为1（待发货）的查询条件
        Example example = new Example(Order.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andEqualTo("orderStatus","1");
        List<Order> orders = orderMapper.selectByExample(example);
        return orders;//向前端返回订单列表
    }

    /**
     * 批量发货
     * @param orderList
     */
    @Transactional
    public void batchSend(List<Order> orderList) {
        //1.判断运单号和物流公司是否为空
        for (Order order : orderList) {
            if (order.getShippingName()==null || order.getShippingCode()==null){
                throw new RuntimeException("请选择快递公司和填写运单号");
            }
        }
        OrderLog orderLog = new OrderLog();
        for (Order order : orderList) {
            //1.修改订单字段
            order.setOrderStatus("2");//已发货 订单状态
            order.setConsignStatus("1");//已发货 发货状态
            order.setConsignTime(new Date());//发货时间
            orderMapper.updateByPrimaryKeySelective(order);
            //2.记录订单日志
            orderLog.setId(idWorker.nextId()+"");//订单日志id
            orderLog.setOrderId(order.getId());//订单id
            orderLog.setConsignStatus(order.getConsignStatus());//发货状态
            orderLog.setOrderStatus(order.getOrderStatus());//订单状态
            orderLogMapper.insertSelective(orderLog);
        }
    }

    /**
     * 订单超时处理
     */
    public void orderTimeOutLogic() {
        //订单超时未付款 自动关闭
        //1.查询超时时间
        OrderConfig orderConfig = orderConfigMapper.selectByPrimaryKey(1);
        Integer orderTimeout = orderConfig.getOrderTimeout();//得到设置好的超时时间 60min
        //2.计算得出超时的时间点 由当前时间点减去超时时间60min
        LocalDateTime localDateTime = LocalDateTime.now().minusMinutes(orderTimeout);
        //3.订单生成时间小于超时时间点的订单都是超时订单
        //3.1设置查询条件：1.订单生成时间小于超时时间点 2.订单状态为待付款的即order_status设置为0 3.is_delete字段设置为 0 未删除
        Example example = new Example(Order.class);
        Example.Criteria criteria = example.createCriteria();
        criteria.andLessThan("createTime",localDateTime);
        criteria.andEqualTo("orderStatus",0);//待付款
        criteria.andEqualTo("isDelete",0);//未删除
        //3.2查询超时订单
        List<Order> orders = orderMapper.selectByExample(example);
        for (Order order : orders) {
            //记录订单变化日志
            OrderLog orderLog = new OrderLog();
            orderLog.setId(idWorker.nextId()+"");
            orderLog.setOperater("system");//系统
            orderLog.setOperateTime(new Date());//设置操作时间
            orderLog.setOrderId(order.getId());//设置orderId
            orderLog.setOrderStatus(order.getOrderStatus());//设置订单状态
            orderLog.setPayStatus(order.getPayStatus());//设置付款状态
            orderLog.setConsignStatus(order.getConsignStatus());//设置发货状态
            orderLog.setRemarks("订单超时关闭，系统自动关闭");
            orderLogMapper.insertSelective(orderLog);//在数据库添加一行数据
            //同时更改订单状态
            order.setOrderStatus("4");//在更新orderLog之后修改被关闭的订单状态为：关闭
            order.setCloseTime(new Date());//设置关闭日期
            orderMapper.updateByPrimaryKeySelective(order);//实现修改
        }
    }

    /**
     * 合并订单
     * @param orderId1
     * @param orderId2
     */
    @Transactional
    @Override
    public void mergeOrder(String orderId1, String orderId2) {
        //1.通过orderId1和orderId2查询到对应的order1和order2
        Order order1 = orderMapper.selectByPrimaryKey(orderId1);
        Order order2 = orderMapper.selectByPrimaryKey(orderId2);
        //2.非空判断
        if (null!=order1 && null!=order2){
            order1.setTotalNum(order1.getTotalNum()+order2.getTotalNum());//将总数合并到order1
            order1.setTotalMoney(order1.getTotalMoney()+order2.getTotalMoney());//将总金额合并到order1
            order1.setPreMoney(order1.getPreMoney()+order2.getPreMoney());//将优惠金额合并到order1
            order1.setPostFee(order1.getPostFee()+order2.getPostFee());//将邮费合并到order1
            order1.setPayMoney(order1.getPayMoney()+order2.getPayMoney());//将实付金额合并到order1
            order1.setUpdateTime(new Date());//设置订单更新时间
            //3.通过order2中的orderId查询到对应的orderItem（订单明细） 即构建条件查询
            Example example = new Example(OrderItem.class);
            Example.Criteria criteria = example.createCriteria();
            criteria.andEqualTo("orderId",order2.getId());
            List<OrderItem> orderItemList = orderItemMapper.selectByExample(example);
            //4.将每一个orderItem中的orderId都设置为order1的id 对orderItem进行非空判断
            if (null!=orderItemList && orderItemList.size()>0){
                for (OrderItem orderItem : orderItemList) {
                    orderItem.setOrderId(order1.getId());
                    orderItemMapper.updateByPrimaryKeySelective(orderItem);//更新到数据库
                }
            }
            //5.将order2中的isDelete字段设置为1，进行逻辑删除
            order2.setIsDelete("1");
            //6.更新到数据库
            orderMapper.updateByPrimaryKeySelective(order1);
            orderMapper.updateByPrimaryKeySelective(order2);
            //7.记录订单日志
            OrderLog orderLog = new OrderLog();
            orderLog.setId(idWorker.nextId()+"");//设置orderLog的id
            orderLog.setOperater("system");//设置处理人员
            orderLog.setOperateTime(new Date());//设置处理日期
            orderLog.setOrderId(order1.getId());//设置订单id
            orderLog.setConsignStatus(order1.getConsignStatus());//设置发货状态
            orderLog.setOrderStatus(order1.getOrderStatus());//设置订单状态
            orderLog.setPayStatus(order1.getPayStatus());//设置付款状态
            orderLog.setRemarks("合并订单");
            orderLogMapper.insertSelective(orderLog);
        }
    }


    /**
     * 回调之后修改订单状态
     * @param orderId
     * @param transactionId
     */
    @Override
    public void updatePayStatus(String orderId, String transactionId) {
        Order order = orderMapper.selectByPrimaryKey(orderId);
        if (order!=null&&"0".equals(order.getPayStatus())){//订单不为空且订单状态为未支付
            //修改订单信息
            order.setPayStatus("1");//修改为已支付
            order.setOrderStatus("1");//订单状态
            order.setUpdateTime(new Date());//更新日期
            order.setPayTime(new Date());//付款日期
            order.setTransactionId(transactionId);//交易流水号
            orderMapper.updateByPrimaryKey(order);//执行修改
            //记录订单日志
            OrderLog orderLog = new OrderLog();
            orderLog.setId(idWorker.nextId()+"");//id
            orderLog.setOperater("system");//操作人
            orderLog.setOperateTime(new Date());//修改日期
            orderLog.setOrderStatus("1");//订单状态
            orderLog.setPayStatus("1");//支付状态
            orderLog.setRemarks("支付流水号："+transactionId);//备注
            orderLog.setOrderId(orderId);//订单id
            orderLogMapper.insert(orderLog);
        }
    }

    @Autowired
    private WeixinPayService weixinPayService;

    @Autowired
    private OrderItemService orderItemService;

    @Reference
    private StockBackService stockBackService;



    /**
     * 通过订单Id 关闭订单
     * @param orderId 订单Id
     */
    @Override
    public void closeOrder(String orderId) {
        //1.关闭订单
        weixinPayService.closeOrder(orderId);
        //2.修改订单状态
        Order order = findById(orderId);
        order.setOrderStatus("4");//订单状态为关闭
        order.setCloseTime(new Date());//设置关闭日期
        orderMapper.updateByPrimaryKeySelective(order);//实现修改
        //3.记录订单日志
        OrderLog orderLog = new OrderLog();
        orderLog.setId(idWorker.nextId()+"");
        orderLog.setOperater("system");//系统
        orderLog.setOperateTime(new Date());//设置操作时间
        orderLog.setOrderId(order.getId());//设置orderId
        orderLog.setOrderStatus(order.getOrderStatus());//设置订单状态
        orderLog.setRemarks("订单超时未支付，系统自动关闭");
        orderLogMapper.insertSelective(orderLog);//在数据库添加一行数据
        //商品库存增加 销量减少 先增加一条回滚数据 再执行回滚
        List<OrderItem> orderItemList = orderItemService.findOrderItemByOrderId(orderId);
        stockBackService.addList(orderItemList);
        stockBackService.doBack();
    }


    /**
     * 构建查询条件
     * @param searchMap
     * @return
     */
    private Example createExample(Map<String, Object> searchMap){
        Example example=new Example(Order.class);
        Example.Criteria criteria = example.createCriteria();
        if(searchMap!=null){
            // 订单id
            if(searchMap.get("id")!=null && !"".equals(searchMap.get("id"))){
                criteria.andLike("id","%"+searchMap.get("id")+"%");
            }
            // 支付类型，1、在线支付、0 货到付款
            if(searchMap.get("payType")!=null && !"".equals(searchMap.get("payType"))){
                criteria.andLike("payType","%"+searchMap.get("payType")+"%");
            }
            // 物流名称
            if(searchMap.get("shippingName")!=null && !"".equals(searchMap.get("shippingName"))){
                criteria.andLike("shippingName","%"+searchMap.get("shippingName")+"%");
            }
            // 物流单号
            if(searchMap.get("shippingCode")!=null && !"".equals(searchMap.get("shippingCode"))){
                criteria.andLike("shippingCode","%"+searchMap.get("shippingCode")+"%");
            }
            // 用户名称
            if(searchMap.get("username")!=null && !"".equals(searchMap.get("username"))){
                criteria.andLike("username","%"+searchMap.get("username")+"%");
            }
            // 买家留言
            if(searchMap.get("buyerMessage")!=null && !"".equals(searchMap.get("buyerMessage"))){
                criteria.andLike("buyerMessage","%"+searchMap.get("buyerMessage")+"%");
            }
            // 是否评价
            if(searchMap.get("buyerRate")!=null && !"".equals(searchMap.get("buyerRate"))){
                criteria.andLike("buyerRate","%"+searchMap.get("buyerRate")+"%");
            }
            // 收货人
            if(searchMap.get("receiverContact")!=null && !"".equals(searchMap.get("receiverContact"))){
                criteria.andLike("receiverContact","%"+searchMap.get("receiverContact")+"%");
            }
            // 收货人手机
            if(searchMap.get("receiverMobile")!=null && !"".equals(searchMap.get("receiverMobile"))){
                criteria.andLike("receiverMobile","%"+searchMap.get("receiverMobile")+"%");
            }
            // 收货人地址
            if(searchMap.get("receiverAddress")!=null && !"".equals(searchMap.get("receiverAddress"))){
                criteria.andLike("receiverAddress","%"+searchMap.get("receiverAddress")+"%");
            }
            // 订单来源：1:web，2：app，3：微信公众号，4：微信小程序  5 H5手机页面
            if(searchMap.get("sourceType")!=null && !"".equals(searchMap.get("sourceType"))){
                criteria.andLike("sourceType","%"+searchMap.get("sourceType")+"%");
            }
            // 交易流水号
            if(searchMap.get("transactionId")!=null && !"".equals(searchMap.get("transactionId"))){
                criteria.andLike("transactionId","%"+searchMap.get("transactionId")+"%");
            }
            // 订单状态
            if(searchMap.get("orderStatus")!=null && !"".equals(searchMap.get("orderStatus"))){
                criteria.andLike("orderStatus","%"+searchMap.get("orderStatus")+"%");
            }
            // 支付状态
            if(searchMap.get("payStatus")!=null && !"".equals(searchMap.get("payStatus"))){
                criteria.andLike("payStatus","%"+searchMap.get("payStatus")+"%");
            }
            // 发货状态
            if(searchMap.get("consignStatus")!=null && !"".equals(searchMap.get("consignStatus"))){
                criteria.andLike("consignStatus","%"+searchMap.get("consignStatus")+"%");
            }
            // 是否删除
            if(searchMap.get("isDelete")!=null && !"".equals(searchMap.get("isDelete"))){
                criteria.andLike("isDelete","%"+searchMap.get("isDelete")+"%");
            }

            // 数量合计
            if(searchMap.get("totalNum")!=null ){
                criteria.andEqualTo("totalNum",searchMap.get("totalNum"));
            }
            // 金额合计
            if(searchMap.get("totalMoney")!=null ){
                criteria.andEqualTo("totalMoney",searchMap.get("totalMoney"));
            }
            // 优惠金额
            if(searchMap.get("preMoney")!=null ){
                criteria.andEqualTo("preMoney",searchMap.get("preMoney"));
            }
            // 邮费
            if(searchMap.get("postFee")!=null ){
                criteria.andEqualTo("postFee",searchMap.get("postFee"));
            }
            // 实付金额
            if(searchMap.get("payMoney")!=null ){
                criteria.andEqualTo("payMoney",searchMap.get("payMoney"));
            }

        }
        return example;
    }

}
