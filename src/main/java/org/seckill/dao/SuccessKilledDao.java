package org.seckill.dao;

import org.apache.ibatis.annotations.Param;
import org.seckill.bean.SuccessKilled;

/**
 * @Author:陈浩杰
 * @description: sad
 * @Date:Created in 13:07 2018/5/21
 */
public interface SuccessKilledDao {
    /**
     * 插入购买明细，可过滤重复（联合主键）
     * @param seckillId
     * @param userPhone
     * @return
     */
    int insertSuccessKilled(@Param("seckillId") long seckillId, @Param("userPhone") long userPhone);

    SuccessKilled queryByIdWithSeckill(@Param("seckillId") long seckillId,@Param("userPhone")long userPhone);
}
