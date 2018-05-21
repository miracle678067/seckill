package org.seckill.dao;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.seckill.bean.Seckill;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Date;
import java.util.List;

/**
 * @Author:陈浩杰
 * @description: 单元测试 首先配置spring和junit整合，junit启动时加载springIOC容器
 * @Date:Created in 20:55 2018/5/21
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:spring/spring-dao.xml"})
public class SeckillDaoTest {
    @Autowired
    private SeckillDao seckillDao;


    @Test
    public void queryById() {
        long id = 1000;
        Seckill seckill = seckillDao.queryById(id);
        System.out.println(seckill.getName());
        System.out.println(seckill);
    }

    @Test
    public void queryAll() {
        List<Seckill> list = seckillDao.queryAll(0,100);
        for (Seckill seckill : list){
            System.out.println(seckill);
        }
    }

    @Test
    public void reduceNumber() {
        Date date = new Date();

        int updateCount = seckillDao.reduceNumber(1000L,date);
        System.out.println("updateCount = " + updateCount);
    }
}