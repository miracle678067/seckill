package org.seckill.dao;

import org.apache.ibatis.annotations.Param;
import org.seckill.bean.Seckill;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @Author:陈浩杰
 * @description: sad
 * @Date:Created in 13:03 2018/5/21
 */
public interface SeckillDao {
    /**
     * 减库存
     * @param seckillId
     * @param killTime
     * @return
     */
    int reduceNumber(@Param("seckillId") long seckillId, @Param("killTime") Date killTime);

    /**
     * 通过id查看秒杀商品
     * @param seckillId
     * @return
     */
    Seckill queryById(long seckillId);

    /**
     * 根据偏移量查询秒杀商品列表
     * @param offset
     * @param limit
     * java没有保存形参的记录，因此在运行期queryall(int offset,int limit)->queryAll(arg0,arg1)，因此加上@Param来告诉实际形参的名字
     */
    List<Seckill> queryAll(@Param("offset") int offset, @Param("limit") int limit);

    /**
     * 使用存储过程执行秒杀
     * @param paramMap
     */
    void killByProcedure(Map<String,Object> paramMap);

}
