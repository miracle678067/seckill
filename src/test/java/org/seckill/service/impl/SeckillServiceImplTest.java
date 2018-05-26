package org.seckill.service.impl;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.seckill.bean.Seckill;
import org.seckill.dto.Exposer;
import org.seckill.dto.SeckillExecution;
import org.seckill.service.SeckillService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

/**
 * @Author:陈浩杰
 * @description: sad
 * @Date:Created in 20:32 2018/5/22
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({"classpath:spring/spring-*.xml"})
public class SeckillServiceImplTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SeckillServiceImplTest.class);
    @Autowired
    private SeckillService seckillService;

    @Test
    public void getSeckillList() {
        List<Seckill> list = seckillService.getSeckillList();
        LOGGER.info("list={}",list);
    }

    @Test
    public void getById() {
        long id = 1000;
        Seckill seckill = seckillService.getById(id);
        LOGGER.info("seckill = {}",seckill);
    }

    @Test
    public void exportSeckillUrl() {
        long id = 1000L;
        Exposer exposer = seckillService.exportSeckillUrl(id);
        LOGGER.info("exposer={}",exposer);
    }

    @Test
    public void executeSeckill() {
        long id = 1000L;
        long userPhone = 145678890021L;
        String md5 = "2de547f429a7bb6af85ddc2af231837d";
        SeckillExecution execution = seckillService.executeSeckill(id,userPhone,md5);
        LOGGER.info("result={}",execution);
    }

    @Test
    public void executeSeckillProcedure() {
        long seckillId = 1001;
        long phone = 12595678067L;
        Exposer exposer = seckillService.exportSeckillUrl(seckillId);
        if (exposer.isExposer()){
            String md5 = exposer.getMd5();
            SeckillExecution execution = seckillService.executeSeckillProcedure(seckillId,phone,md5);
            LOGGER.info(execution.getStateInfo());
        }
    }
}