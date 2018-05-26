package org.seckill.dao.cache;

import com.dyuproject.protostuff.LinkedBuffer;
import com.dyuproject.protostuff.ProtobufIOUtil;
import com.dyuproject.protostuff.runtime.RuntimeSchema;
import org.seckill.bean.Seckill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * @Author:陈浩杰
 * @description: sad
 * @Date:Created in 18:59 2018/5/26
 */
public class RedisDao {
    private final Logger logger = LoggerFactory.getLogger(RedisDao.class);
    private final JedisPool jedisPool;
    private RuntimeSchema<Seckill> schema = RuntimeSchema.createFrom(Seckill.class);
    public RedisDao(String ip,int port) {
        jedisPool = new JedisPool(ip,port);
    }

    public Seckill getSeckill(long seckillId){
        //redis操作逻辑
        try {
            Jedis jedis = jedisPool.getResource();
            try {
                String key = "seckill:"+seckillId;

                //redis内部没有实现序列化操作，因此需要进行自定义序列化
                //get->拿到字节数组bytes[]->进行反序列化—>Object
                byte[] bytes = jedis.get(key.getBytes());
                if (bytes != null){
                    //空对象
                    Seckill seckill = schema.newMessage();
                    //seckill被反序列化
                    ProtobufIOUtil.mergeFrom(bytes,seckill,schema);
                }
            } finally {
                jedis.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String putSeckill(Seckill seckill){
        try {
            Jedis jedis = jedisPool.getResource();
            //set Object->序列化->bytes[]
            try {
                String key = "seckill:"+seckill.getSeckillId();
                byte[] bytes = ProtobufIOUtil.toByteArray(seckill,schema,
                        LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE));

                //缓存超时
                int timeout = 60*60;
                String result = jedis.setex(key.getBytes(),timeout,bytes);
                return result;
            } finally {
                jedis.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
