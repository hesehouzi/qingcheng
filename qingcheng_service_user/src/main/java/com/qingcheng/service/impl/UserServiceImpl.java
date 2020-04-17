package com.qingcheng.service.impl;
import com.alibaba.dubbo.config.annotation.Service;
import com.alibaba.fastjson.JSON;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.qingcheng.dao.UserMapper;
import com.qingcheng.entity.PageResult;
import com.qingcheng.pojo.user.User;
import com.qingcheng.service.user.UserService;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import tk.mybatis.mapper.entity.Example;

import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    /**
     * 返回全部记录
     * @return
     */
    public List<User> findAll() {
        return userMapper.selectAll();
    }

    /**
     * 分页查询
     * @param page 页码
     * @param size 每页记录数
     * @return 分页结果
     */
    public PageResult<User> findPage(int page, int size) {
        PageHelper.startPage(page,size);
        Page<User> users = (Page<User>) userMapper.selectAll();
        return new PageResult<User>(users.getTotal(),users.getResult());
    }

    /**
     * 条件查询
     * @param searchMap 查询条件
     * @return
     */
    public List<User> findList(Map<String, Object> searchMap) {
        Example example = createExample(searchMap);
        return userMapper.selectByExample(example);
    }

    /**
     * 分页+条件查询
     * @param searchMap
     * @param page
     * @param size
     * @return
     */
    public PageResult<User> findPage(Map<String, Object> searchMap, int page, int size) {
        PageHelper.startPage(page,size);
        Example example = createExample(searchMap);
        Page<User> users = (Page<User>) userMapper.selectByExample(example);
        return new PageResult<User>(users.getTotal(),users.getResult());
    }

    /**
     * 根据Id查询
     * @param username
     * @return
     */
    public User findById(String username) {
        return userMapper.selectByPrimaryKey(username);
    }

    /**
     * 新增
     * @param user
     */
    public void add(User user) {
        userMapper.insert(user);
    }

    /**
     * 修改
     * @param user
     */
    public void update(User user) {
        userMapper.updateByPrimaryKeySelective(user);
    }

    /**
     *  删除
     * @param username
     */
    public void delete(String username) {
        userMapper.deleteByPrimaryKey(username);
    }

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 发送短信验证码
     * @param phone 手机号
     */
    public void sendSMS(String phone) {
        //1.得到六位数短信验证码
        Random random = new Random();//随机对象
        int code = random.nextInt(999999);//生成验证码
        if (code<100000){
            code=code+100000;
        }
        System.out.println("短信验证码："+code);
        //2.将验证码保存到redis
        redisTemplate.boundValueOps("code_"+phone).set(code+"");
        redisTemplate.boundValueOps("code_"+phone).expire(5, TimeUnit.MINUTES);//设置过期时间5min

        //3.发送给RabbitMq
        HashMap<String, String> map = new HashMap<String, String>();
        map.put("phone",phone);
        map.put("code",code+"");
        rabbitTemplate.convertAndSend("","queue.sms", JSON.toJSONString(map));
    }

    /**
     * 用户注册
     * @param user 用户
     * @param smsCode 验证码
     */
    public void add(User user, String smsCode) {
        //1.验证码校验
        //1.1从缓存提取验证码
        String sysCode = (String) redisTemplate.boundValueOps("code_" + user.getPhone()).get();//提取系统发出的验证码:sysCode
        //1.2验证码判断
        if (sysCode==null){
            throw new RuntimeException("验证码未发送或已过期！！！");
        }
        if (!sysCode.equals(smsCode)){
            throw new RuntimeException("验证码不正确！！！");
        }
        if (user.getName()==null){
            user.setUsername(user.getPhone());//如果用户注册的时候没有填写用户名 那么使用其填写的手机号码作为用户名
        }
        //1.3校验用户名是否已经注册
        User searchUser = new User();
        searchUser.setUsername(user.getUsername());
        if (userMapper.selectCount(searchUser)>0){//这里进行数据库查询 意思是：与用户填写的用户名相同的用户有多少 如果大于0 表示用户名重复

            throw new RuntimeException("该手机号已注册！！！");

        }
        //2.数据添加 user字段设置
        user.setCreated(new Date());//设置创建日期
        user.setUpdated(new Date());//设置更新时间
        user.setPoints(0);//设置积分
        user.setIsEmailCheck("0");//邮箱验证
        user.setIsMobileCheck("1");//手机号验证
        userMapper.insert(user);
    }

    /**
     * 构建查询条件
     * @param searchMap
     * @return
     */
    private Example createExample(Map<String, Object> searchMap){
        Example example=new Example(User.class);
        Example.Criteria criteria = example.createCriteria();
        if(searchMap!=null){
            // 用户名
            if(searchMap.get("username")!=null && !"".equals(searchMap.get("username"))){
                criteria.andLike("username","%"+searchMap.get("username")+"%");
            }
            // 密码，加密存储
            if(searchMap.get("password")!=null && !"".equals(searchMap.get("password"))){
                criteria.andLike("password","%"+searchMap.get("password")+"%");
            }
            // 注册手机号
            if(searchMap.get("phone")!=null && !"".equals(searchMap.get("phone"))){
                criteria.andLike("phone","%"+searchMap.get("phone")+"%");
            }
            // 注册邮箱
            if(searchMap.get("email")!=null && !"".equals(searchMap.get("email"))){
                criteria.andLike("email","%"+searchMap.get("email")+"%");
            }
            // 会员来源：1:PC，2：H5，3：Android，4：IOS
            if(searchMap.get("sourceType")!=null && !"".equals(searchMap.get("sourceType"))){
                criteria.andLike("sourceType","%"+searchMap.get("sourceType")+"%");
            }
            // 昵称
            if(searchMap.get("nickName")!=null && !"".equals(searchMap.get("nickName"))){
                criteria.andLike("nickName","%"+searchMap.get("nickName")+"%");
            }
            // 真实姓名
            if(searchMap.get("name")!=null && !"".equals(searchMap.get("name"))){
                criteria.andLike("name","%"+searchMap.get("name")+"%");
            }
            // 使用状态（1正常 0非正常）
            if(searchMap.get("status")!=null && !"".equals(searchMap.get("status"))){
                criteria.andLike("status","%"+searchMap.get("status")+"%");
            }
            // 头像地址
            if(searchMap.get("headPic")!=null && !"".equals(searchMap.get("headPic"))){
                criteria.andLike("headPic","%"+searchMap.get("headPic")+"%");
            }
            // QQ号码
            if(searchMap.get("qq")!=null && !"".equals(searchMap.get("qq"))){
                criteria.andLike("qq","%"+searchMap.get("qq")+"%");
            }
            // 手机是否验证 （0否  1是）
            if(searchMap.get("isMobileCheck")!=null && !"".equals(searchMap.get("isMobileCheck"))){
                criteria.andLike("isMobileCheck","%"+searchMap.get("isMobileCheck")+"%");
            }
            // 邮箱是否检测（0否  1是）
            if(searchMap.get("isEmailCheck")!=null && !"".equals(searchMap.get("isEmailCheck"))){
                criteria.andLike("isEmailCheck","%"+searchMap.get("isEmailCheck")+"%");
            }
            // 性别，1男，0女
            if(searchMap.get("sex")!=null && !"".equals(searchMap.get("sex"))){
                criteria.andLike("sex","%"+searchMap.get("sex")+"%");
            }

            // 会员等级
            if(searchMap.get("userLevel")!=null ){
                criteria.andEqualTo("userLevel",searchMap.get("userLevel"));
            }
            // 积分
            if(searchMap.get("points")!=null ){
                criteria.andEqualTo("points",searchMap.get("points"));
            }
            // 经验值
            if(searchMap.get("experienceValue")!=null ){
                criteria.andEqualTo("experienceValue",searchMap.get("experienceValue"));
            }

        }
        return example;
    }

}
