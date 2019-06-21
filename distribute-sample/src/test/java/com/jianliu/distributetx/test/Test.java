//package com.jianliu.distributetx.test;
//
//import com.jianliu.distributetx.Application;
//import com.jianliu.distributetx.BaseLogger;
//import com.jianliu.distributetx.sample.TestService;
//import org.junit.runner.RunWith;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.test.context.junit4.SpringRunner;
//
//import javax.annotation.Resource;
//
///**
// * distribute-tx
// *
// * @author jian.liu
// * @since 2019/6/19
// */
//@RunWith(SpringRunner.class)
//@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
//public class Test extends BaseLogger {
//
//    @Resource
//    private TestService testService;
//
//    @org.junit.Test
//    public void test() {
//        testService.doTransaction(1);
//    }
//
//}
