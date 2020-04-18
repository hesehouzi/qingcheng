package com.qingcheng.entity;

import com.qingcheng.pojo.goods.Goods;
import com.qingcheng.pojo.user.User;

public class Dto {
    private User user;
    private Goods goods;

    public Dto(User user, Goods goods) {
        this.user = user;
        this.goods = goods;
    }

    public Dto() {
    }
}
