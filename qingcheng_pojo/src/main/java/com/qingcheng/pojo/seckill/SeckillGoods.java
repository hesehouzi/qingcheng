package com.qingcheng.pojo.seckill;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
@Data
@Table(name = "tb_seckill_goods")
public class SeckillGoods implements Serializable {

    @Id
    @Column(name = "id")
    private Long id;//id

    @Column(name = "goods_id")
    private Long goodsId;//spuId

    @Column(name = "item_id")
    private Long itemId;//skuId

    @Column(name = "title")
    private String title;//标题

    @Column(name = "small_pic")
    private String smallPic;//商品图片

    @Column(name = "price")
    private BigDecimal price;//原价格

    @Column(name = "cost_price")
    private BigDecimal costPrice;//秒杀价格

    @Column(name = "seller_id")
    private String sellerId;//商家Id

    @Column(name = "create_time")
    private Date createTime;//添加日期

    @Column(name = "check_time")
    private Date checkTime;//审核日期

    @Column(name = "status")
    private String status;//审核状态

    @Column(name = "start_time")
    private Date startTime;//开始时间

    @Column(name = "end_time")
    private Date endTime;//结束时间

    @Column(name = "num")
    private Integer num;//秒杀商品数量

    @Column(name = "stock_count")
    private Integer stockCount;//剩余库存数

    @Column(name = "introduction")
    private String introduction;//描述


}
