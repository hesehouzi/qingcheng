package com.qingcheng.entity;

import com.qingcheng.pojo.goods.Goods;
import com.qingcheng.pojo.user.User;

public class Dto {
    private User user;
    private Goods goods;

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Goods getGoods() {
        return goods;
    }

    public void setGoods(Goods goods) {
        this.goods = goods;
    }
}
