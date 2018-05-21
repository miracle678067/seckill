package org.seckill.exception;

/**
 * @Author:陈浩杰
 * @description: 秒杀关闭异常
 * @Date:Created in 21:57 2018/5/21
 */
public class SeckillCloseException extends SeckillException{
    public SeckillCloseException(String message) {
        super(message);
    }

    public SeckillCloseException(String message, Throwable cause) {
        super(message, cause);
    }
}
