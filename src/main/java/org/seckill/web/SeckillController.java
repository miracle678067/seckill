package org.seckill.web;

import org.seckill.bean.Seckill;
import org.seckill.dto.Exposer;
import org.seckill.dto.SeckillExecution;
import org.seckill.dto.SeckillResult;
import org.seckill.enums.SeckillStateEnum;
import org.seckill.exception.RepeatKillException;
import org.seckill.exception.SeckillCloseException;
import org.seckill.exception.SeckillException;
import org.seckill.service.SeckillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.Date;
import java.util.List;

/**
 * @Author:陈浩杰
 * @description: sad
 * @Date:Created in 16:49 2018/5/24
 */
@Controller
@RequestMapping(value = "/seckill")
public class SeckillController {
    @Autowired
    private SeckillService seckillService;

    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public String getList(Long seckillId, Model model) {
        List<Seckill> list = seckillService.getSeckillList();
        model.addAttribute("list", list);
        System.out.println("asdjk");
        return "list";
    }

    @RequestMapping(value = "/{seckillId}/detail", method = RequestMethod.GET)
    public String detail(@PathVariable("seckillId") Long seckillId, Model model) {
        if (seckillId == null) {
            return "redirect:/seckill/list";
        }
        Seckill seckill = seckillService.getById(seckillId);
        if (seckill == null) {
            return "forward:/seckill/list";
        }
        model.addAttribute("seckill", seckill);
        return "detail";
    }

    @RequestMapping(value = "/{seckillId}/exposer", method = RequestMethod.POST)
    @ResponseBody
    public SeckillResult<Exposer> exposer(@PathVariable("seckillId") Long seckillId) {
        SeckillResult<Exposer> result;
        try {
            //暴露秒杀接口地址
            Exposer exposer = seckillService.exportSeckillUrl(seckillId);
            System.out.println("seckillId:" + seckillId);
            result = new SeckillResult<>(true, exposer);
        } catch (Exception e) {
            e.printStackTrace();
            result = new SeckillResult<>(false, e.getMessage());
        }
        System.out.println("返回结果：" + result.getData());
        return result;
    }

    @RequestMapping(value = "/{seckillId}/{md5}/execution", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public SeckillResult<SeckillExecution> execute(@PathVariable("seckillId") Long seckillId, @PathVariable("md5") String md5, @CookieValue(value = "killPhone", required = false) Long userPhone) {
        SeckillResult<SeckillExecution> result;
        System.out.println("执行秒杀" + md5);
        if (userPhone == null) {
            return new SeckillResult<>(false, "未注册");
        }
        try {
            //SeckillExecution execution = seckillService.executeSeckill(seckillId, userPhone, md5);
           //通过存储过程执行
            SeckillExecution execution = seckillService.executeSeckillProcedure(seckillId,userPhone,md5);
            return new SeckillResult<>(true, execution);
        } catch (RepeatKillException e) {
            SeckillExecution execution = new SeckillExecution(seckillId, SeckillStateEnum.REPEAT_KILL);
            System.out.println("重复秒杀");
            return new SeckillResult<>(true, execution);
        } catch (SeckillCloseException e) {
            SeckillExecution execution = new SeckillExecution(seckillId, SeckillStateEnum.END);
            System.out.println("秒杀已结束");
            return new SeckillResult<>(true, execution);
        } catch (SeckillException e) {
            SeckillExecution execution = new SeckillExecution(seckillId, SeckillStateEnum.INNER_ERROR);
            System.out.println("内部错误");
            return new SeckillResult<>(true, execution);
        }
    }
    @RequestMapping(value = "/time/now",method = RequestMethod.GET)
    @ResponseBody
    public SeckillResult<Long> time(){
        Date now = new Date();
        System.out.println(new SeckillResult(true,now.getTime()));
        return new SeckillResult(true,now.getTime());
    }
}
