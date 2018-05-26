/*存放主要交互逻辑js代码*/

var seckill = {
    URL: {
        now: function () {
            return '/seckill/time/now';
        },
        exposer:function (seckillId) {
            return '/seckill/'+seckillId+'/exposer';
        },
        execution:function (seckillId,md5) {
            return '/seckill/' +seckillId+'/'+md5+'/execution'
        }
    },
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
    validatePhone: function (phone) {
        //isNaN表示是否非数字
        if (phone && phone.length == 11 && !isNaN(phone)) {
            return true;
        } else {
            return false;
        }
    },
    countDown: function (seckillId, nowTime, startTime, endTime) {
        var seckillBox = $('#seckill-box');
        if (nowTime > endTime) {
            seckillBox.html('秒杀结束');
        }else if (nowTime < startTime){
            //秒杀未开始，倒计时
            var killTime = new Date(startTime);

            seckillBox.countdown(killTime,function (event) {
                var format = event.strftime('秒杀倒计时：%D天 %H时 %M分 %S秒');
                seckillBox.html(format)
            }).on('finish.countdown',function () {
                //获取秒杀地址 控制显示逻辑 执行秒杀
                seckill.handleSeckillkill(seckillId,seckillBox);
            });
        } else {
            seckill.handleSeckillkill(seckillId,seckillBox);
        }
    },
    detail: {
        init: function (params) {
            //在cookie查找手机号
            var killPhone = $.cookie('killPhone');

            if (!seckill.validatePhone(killPhone)) {
                //绑定phone
                var killPhoneModal = $('#killPhoneModal');
                killPhoneModal.modal({

                    show: true, //显示弹出层
                    backdrop: 'static',//禁止位置关闭
                    keyboard: false//关闭键盘事件
                });
                $('#killPhoneBtn').click(function () {
                    var inputPhone = $('#killPhoneKey').val();
                    console.log(inputPhone);
                    if (seckill.validatePhone(inputPhone)) {

                        //电话写入cookie
                        $.cookie('killPhone', inputPhone, {expires: 7, path: '/seckill'});
                        //刷新页面
                        window.location.reload();
                    } else {
                        $('#killPhoneMessage').hide().html('<label class="label label-danger">手机号错误！</label>').show(300);
                    }
                });
            }
            //已经登录,ajax计时交互
            var startTime = params['startTime'];
            var endTime = params['endTime'];
            var seckillId = params['seckillId'];
            $.get(seckill.URL.now(), {}, function (result) {
                var  obj = eval("("+result+")");
                if (obj && obj.success) {
                    var nowTime = obj.data;
                    //时间判断
                    seckill.countDown(seckillId, nowTime, startTime, endTime);
                } else {
                    console.log('result:' + obj.success);
                    console.log('result:' + result);
                }
            })
        }
    }
}