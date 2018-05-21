package org.seckill.exception;

/**
 * @Author:陈浩杰
 * @description: 重复秒杀异常（运行期异常）
 * @Date:Created in 21:56 2018/5/21
 */
public class RepeatKillException extends SeckillException{
    public RepeatKillException(String message) {
        super(message);
    }

    public RepeatKillException(String message, Throwable cause) {
        super(message, cause);
    }
}
