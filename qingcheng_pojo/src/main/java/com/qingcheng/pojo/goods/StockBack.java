package com.qingcheng.pojo.goods;

import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Date;
@Table(name = "tb_stock_back")
public class StockBack {

    @Id
    private String orderId;//订单id

    @Id
    private String skuId;//skuId

    private Integer num;//回滚数量

    private String status;//回滚状态

    private Date createTime;//创建时间

    private Date backTime;//回滚时间

    public StockBack() {
    }

    public StockBack(String orderId, String skuId, Integer num, String status, Date createTime, Date backTime) {
        this.orderId = orderId;
        this.skuId = skuId;
        this.num = num;
        this.status = status;
        this.createTime = createTime;
        this.backTime = backTime;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getSkuId() {
        return skuId;
    }

    public void setSkuId(String skuId) {
        this.skuId = skuId;
    }

    public Integer getNum() {
        return num;
    }

    public void setNum(Integer num) {
        this.num = num;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getBackTime() {
        return backTime;
    }

    public void setBackTime(Date backTime) {
        this.backTime = backTime;
    }
}
