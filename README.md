# 使用SSM实现秒杀商品案例
## 实现功能

- 秒杀接口暴露
- 执行秒杀
- 相关查询

## 数据库设计与编码

```
--创建数据库脚本
CREATE  DATABASE seckill;
--使用数据库
use seckill;
--创建秒杀库存表
CREATE TABLE seckill(
  `seckill_id` bigint NOT NULL AUTO_INCREMENT COMMENT '商品库存id',
  `name` varchar (120) NOT NULL COMMENT '商品名称',
  `number` int NOT NULL COMMENT '库存数量',
  `start_time` timestamp NOT NULL COMMENT '秒杀开启时间',
  `end_time` timestamp NOT NULL COMMENT '秒杀结束时间',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY(seckill_id),
  key idx_start_time(start_time),
  key idx_end_time(end_time),
  key idx_create_time(create_time)
)ENGINE=InnoDB AUTO_INCREMENT=1000 DEFAULT CHARSET=utf8 COMMENT='秒杀库存表';

--初始化数据
insert into
  seckill(name,number,start_time,end_time)
  values
    ('1元秒杀坚果tNT工作站',100,'2018-06-01 00:00:00','2018-06-02 00:00:00'),
    ('1元秒杀iphonex',100,'2018-06-01 00:00:00','2018-06-02 00:00:00'),
    ('1元秒杀坚果3',100,'2018-06-01 00:00:00','2018-06-02 00:00:00'),
    ('1元秒杀mac',100,'2018-06-01 00:00:00','2018-06-02 00:00:00');

--秒杀成功明细表
--用户登录认证的相关的信息
CREATE TABLE success_killed(
  `seckill_id` bigint NOT NULL COMMENT '秒杀商品id',
  `user_phone` bigint NOT NULL COMMENT '用户手机号',
  `state` bigint NOT NULL DEFAULT -1 COMMENT '状态表示：-1：无效，0：成功，1：已付款',
  `create_time` timestamp NOT NULL COMMENT '创建时间',
  PRIMARY KEY (seckill_id,user_phone),
  key idx_create_time(create_time)
)ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='秒杀成功明细表';
```

## 设计Dao接口

`SeckillDao接口`

```
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
}
```

`SuccessKilledDao`

```
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
```

## 设计Service层接口及实现

`SeckillService接口`

```
public interface SeckillService {

    /**
     * 查询所有秒杀列表
     * @return
     */
    List<Seckill> getSeckillList();

    /**
     * 查询单个秒杀记录
     * @param seckillId
     * @return
     */
    Seckill getById(long seckillId);

    /**
     * 输出秒杀接口的地址
     * 秒杀开启时输出接口地址
     * 否则输出秒杀时间和系统时间
     * @param seckillId
     */
    Exposer exportSeckillUrl(long seckillId);

    /**
     * 执行秒杀操作
     * @param seckillId
     * @param userPhone
     * @param md5 匹配md5是否一致，判断用户秒杀地址是否正常
     */
    SeckillExecution executeSeckill(long seckillId, long userPhone, String md5) throws SeckillException,RepeatKillException,SeckillCloseException;
}
```

### 配置和开启事务(抛出运行期异常时则进行事务回滚)

```
<!--spring-service.xml-->
    <!--配置事务管理器-->
    <bean id="transactionManager" class="org.springframework.jdbc.datasource.DataSourceTransactionManager">
        <property name="dataSource" ref="dataSource"/>
    </bean>

    <!--配置基于注解的声明式事务-->
    <tx:annotation-driven transaction-manager="transactionManager"/>
```

```
	 @Override
    @Transactional
    public SeckillExecution executeSeckill(long seckillId, long userPhone, String md5) throws SeckillException, RepeatKillException, SeckillCloseException {
        if (md5 == null || !md5.equals(getMd5(seckillId))) {
            throw new SeckillException("seckill data rewrite");
        }
        //执行秒杀逻辑：减库存 + 记录购买行为
        Date nowTime = new Date();
        try {
            int updateCount = seckillDao.reduceNumber(seckillId, nowTime);
            if (updateCount <= 0) {
                //没有更新到记录
                throw new SeckillCloseException("seckill is closed");
            } else {
                //记录购买行为
                int insertCount = successKilledDao.insertSuccessKilled(seckillId, userPhone);
                if (insertCount <= 0) {
                    //重复秒杀
                    throw new RepeatKillException("seckill repeated");
                } else {
                    SuccessKilled successKilled = successKilledDao.queryByIdWithSeckill(seckillId, userPhone);
                    return new SeckillExecution(seckillId, SeckillStateEnum.SUCCESS, successKilled);
                }
            }
        } catch (SeckillCloseException e1) {
            throw e1;
        } catch (RepeatKillException e2) {
            throw e2;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            //所有编译器异常转化为运行期异常
            throw new SeckillException("seckill inner error:" + e.getMessage());
        }
    }
```

## web层设计 

- URL设计

  `/模块/资源/{标示}/集合1/...`

​       `    GET /seckill/list  秒杀列表   `

​       `  GET /seckill/{id}/detail 详情页 `

​       `GET /seckill/time/now 系统时间 `

​       `POST /seckill/{id}/exposer 暴露秒杀`

​       `POST /seckill/{id}/{md5}/execution 执行秒杀 `

- 获取秒杀列表


```
 @RequestMapping(value = "/list", method = RequestMethod.GET)
    public String getList(Long seckillId, Model model) {
        List<Seckill> list = seckillService.getSeckillList();
        model.addAttribute("list", list);
        System.out.println("asdjk");
        return "list";
    }
```

- 商品详情页

```
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
```

- 暴露秒杀接口地址

```
@RequestMapping(value = "/{seckillId}/exposer", method = RequestMethod.POST)
    @ResponseBody
    public SeckillResult<Exposer> exposer(@PathVariable("seckillId") Long seckillId) {
        SeckillResult<Exposer> result;
        try {
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
```

- 执行秒杀操作

```
 @RequestMapping(value = "/{seckillId}/{md5}/execution", method = RequestMethod.POST, produces = "application/json;charset=UTF-8")
    @ResponseBody
    public SeckillResult<SeckillExecution> execute(@PathVariable("seckillId") Long seckillId, @PathVariable("md5") String md5, @CookieValue(value = "killPhone", required = false) Long userPhone) {
        SeckillResult<SeckillExecution> result;
        System.out.println("执行秒杀" + md5);
        if (userPhone == null) {
            return new SeckillResult<>(false, "未注册");
        }
        try {
            SeckillExecution execution = seckillService.executeSeckill(seckillId, userPhone, md5);
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
```

- 显示当前系统时间

```
 @RequestMapping(value = "/time/now",method = RequestMethod.GET)
    @ResponseBody
    public SeckillResult<Long> time(){
        Date now = new Date();
        System.out.println(new SeckillResult(true,now.getTime()));
        return new SeckillResult(true,now.getTime());
    }
```

- 处理秒杀操作逻辑

```
handleSeckillkill:function(seckillId,node){
        node.hide().html('<button class="btn btn-primary btn-lg" id="killBtn">开始秒杀</button>');
        $.post(seckill.URL.exposer(seckillId),{},function (result) {
            //在回调函数中执行交互流程
            var result =  eval("("+result+")")
            console.log("得到结果"+result['success'] + seckillId)
            if (result && result['success']){
                var exposer = result['data'];
                console.log(exposer)
                if (exposer['exposer']) {
                    //开启秒杀
                    //获取秒杀地址
                    var md5 = exposer['md5'];
                    var killUrl = seckill.URL.execution(seckillId,md5);
                    console.log("killUrl : " + killUrl)
                    //绑定一次点击事件，降低服务器端的压力
                    $('#killBtn').one('click',function () {
                        //执行秒杀执行的操作
                        //1.先禁用按钮
                        $(this).addClass('disable');
                        //2.发送秒杀请求执行秒杀
                        $.post(killUrl,{},function (result) {
                            //var result = eval("("+result+")");
                            console.log('result:'+result);
                            if (result && result['success']){
                                var killResult = result['data'];
                                console.log(killResult);
                                var state = killResult['state'];
                                var stateInfo = killResult['stateInfo'];
                                node.html('<span class="label label-success">' + stateInfo + '</span>')

                            }
                        });
                    });
                    node.show();
                }else {
                    //未开启秒杀
                    var now = exposer['now'];
                    var start = exposer['startTime'];
                    var end = exposer['endTime'];
                    //为防止计时结束，但未开始秒杀的情况，重新计算计时逻辑
                    seckill.countDown(seckillId,now,start,end);
                }
            }else {
                console.log('result:' + result);
            }
        })
    },
```

## 遇到的问题：

- @Responsebody返回乱码

**解决方法：**

- ```
  org.springframework.beans.factory.BeanCreationException: Error creating bean with name 'org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter': Instantiation of bean failed; nested exception is org.springframework.beans.BeanInstantiationException: Failed to instantiate [org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter]: Constructor threw exception; nested exception is java.lang.NoClassDefFoundError: com/fasterxml/jackson/databind/exc/InvalidDefinitionException
  	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.instantiateBean(AbstractAutowireCapableBeanFactory.java:1232)
  	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBeanInstance(AbstractAutowireCapableBeanFactory.java:1131)
  	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.doCreateBean(AbstractAutowireCapableBeanFactory.java:545)
  	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.createBean(AbstractAutowireCapableBeanFactory.java:502)
  	at org.springframework.beans.factory.support.AbstractBeanFactory.lambda$doGetBean$0(AbstractBeanFactory.java:312)
  	at org.springframework.beans.factory.support.DefaultSingletonBeanRegistry.getSingleton(DefaultSingletonBeanRegistry.java:228)
  	at org.springframework.beans.factory.support.AbstractBeanFactory.doGetBean(AbstractBeanFactory.java:310)
  	at org.springframework.beans.factory.support.AbstractBeanFactory.getBean(AbstractBeanFactory.java:200)
  	at org.springframework.beans.factory.support.DefaultListableBeanFactory.preInstantiateSingletons(DefaultListableBeanFactory.java:761)
  	at org.springframework.context.support.AbstractApplicationContext.finishBeanFactoryInitialization(AbstractApplicationContext.java:868)
  	at org.springframework.context.support.AbstractApplicationContext.refresh(AbstractApplicationContext.java:549)
  	at org.springframework.web.servlet.FrameworkServlet.configureAndRefreshWebApplicationContext(FrameworkServlet.java:676)
  	at org.springframework.web.servlet.FrameworkServlet.createWebApplicationContext(FrameworkServlet.java:642)
  	at org.springframework.web.servlet.FrameworkServlet.createWebApplicationContext(FrameworkServlet.java:690)
  	at org.springframework.web.servlet.FrameworkServlet.initWebApplicationContext(FrameworkServlet.java:558)
  	at org.springframework.web.servlet.FrameworkServlet.initServletBean(FrameworkServlet.java:499)
  	at org.springframework.web.servlet.HttpServletBean.init(HttpServletBean.java:172)
  	at javax.servlet.GenericServlet.init(GenericServlet.java:160)
  	at org.apache.catalina.core.StandardWrapper.initServlet(StandardWrapper.java:1280)
  	at org.apache.catalina.core.StandardWrapper.loadServlet(StandardWrapper.java:1193)
  	at org.apache.catalina.core.StandardWrapper.allocate(StandardWrapper.java:865)
  	at org.apache.catalina.core.StandardWrapperValve.invoke(StandardWrapperValve.java:136)
  	at org.apache.catalina.core.StandardContextValve.invoke(StandardContextValve.java:123)
  	at org.apache.catalina.core.StandardHostValve.invoke(StandardHostValve.java:171)
  	at org.apache.catalina.valves.ErrorReportValve.invoke(ErrorReportValve.java:100)
  	at org.apache.catalina.valves.AccessLogValve.invoke(AccessLogValve.java:953)
  	at org.apache.catalina.core.StandardEngineValve.invoke(StandardEngineValve.java:118)
  	at org.apache.catalina.connector.CoyoteAdapter.service(CoyoteAdapter.java:408)
  	at org.apache.coyote.http11.AbstractHttp11Processor.process(AbstractHttp11Processor.java:1041)
  	at org.apache.coyote.AbstractProtocol$AbstractConnectionHandler.process(AbstractProtocol.java:603)
  	at org.apache.tomcat.util.net.JIoEndpoint$SocketProcessor.run(JIoEndpoint.java:310)
  	at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1142)
  	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:617)
  	at java.lang.Thread.run(Thread.java:745)
  Caused by: org.springframework.beans.BeanInstantiationException: Failed to instantiate [org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter]: Constructor threw exception; nested exception is java.lang.NoClassDefFoundError: com/fasterxml/jackson/databind/exc/InvalidDefinitionException
  	at org.springframework.beans.BeanUtils.instantiateClass(BeanUtils.java:182)
  	at org.springframework.beans.factory.support.SimpleInstantiationStrategy.instantiate(SimpleInstantiationStrategy.java:87)
  	at org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory.instantiateBean(AbstractAutowireCapableBeanFactory.java:1224)
  	... 33 more
  Caused by: java.lang.NoClassDefFoundError: com/fasterxml/jackson/databind/exc/InvalidDefinitionException
  	at org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter.<init>(AllEncompassingFormHttpMessageConverter.java:74)
  	at org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.<init>(RequestMappingHandlerAdapter.java:189)
  	at sun.reflect.NativeConstructorAccessorImpl.newInstance0(Native Method)
  	at sun.reflect.NativeConstructorAccessorImpl.newInstance(NativeConstructorAccessorImpl.java:62)
  	at sun.reflect.DelegatingConstructorAccessorImpl.newInstance(DelegatingConstructorAccessorImpl.java:45)
  	at java.lang.reflect.Constructor.newInstance(Constructor.java:423)
  	at org.springframework.beans.BeanUtils.instantiateClass(BeanUtils.java:170)
  	... 35 more
  Caused by: java.lang.ClassNotFoundException: com.fasterxml.jackson.databind.exc.InvalidDefinitionException
  	at org.apache.catalina.loader.WebappClassLoader.loadClass(WebappClassLoader.java:1702)
  	at org.apache.catalina.loader.WebappClassLoader.loadClass(WebappClassLoader.java:1547)
  	... 42 more
  ```

**解决方法：**在pom.xml添加如下的依赖，我使用spring5.0.3版本，因此要添加version 2.8及以上

```
 <dependency>
          <groupId>com.fasterxml.jackson.core</groupId>
          <artifactId>jackson-core</artifactId>
        <version>2.9.2</version>
      </dependency>
    <dependency>
      <groupId>com.fasterxml.jackson.core</groupId>
      <artifactId>jackson-annotations</artifactId>
      <version>2.9.2</version>
    </dependency>
    <dependency>
      <groupId>com.fasterxml.jackson.core</groupId>
      <artifactId>jackson-databind</artifactId>
      <version>2.9.2</version>
    </dependency>
    <dependency>
      <groupId>com.fasterxml.jackson.module</groupId>
      <artifactId>jackson-module-jaxb-annotations</artifactId>
      <version>2.9.2</version>
    </dependency>
```

- ajax获取json数据成功，取值为undefined

**解决方法：**

​        如果jquery异步请求没做类型说明，或者以字符串方式接受，那么需要做一次对象化处理，将该字符串放于eval()中执行一次，如`var result =  eval("("+result+")")`

**原因：**

​        eval本身的问题。 由于json是以”{}”的方式来开始以及结束的，在JS中，它会被当成一个语句块来处理，所以必须强制性的将它转换成一种表达式。