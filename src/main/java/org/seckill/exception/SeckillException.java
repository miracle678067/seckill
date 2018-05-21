package org.seckill.exception;

/**
 * @Author:陈浩杰
 * @description: 秒杀相关异常
 * @Date:Created in 21:59 2018/5/21
 */
public class SeckillException extends RuntimeException{
    public SeckillException(String message) {
        super(message);
    }

    public SeckillException(String message, Throwable cause) {
        super(message, cause);
    }
}
