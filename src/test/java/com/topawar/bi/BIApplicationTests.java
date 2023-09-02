package com.topawar.bi;

import com.yupi.yucongming.dev.client.YuCongMingClient;
import com.yupi.yucongming.dev.common.BaseResponse;
import com.yupi.yucongming.dev.model.DevChatRequest;
import com.yupi.yucongming.dev.model.DevChatResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

/**
 * 主类测试
 *
 * @author topawar
 * @from <a href="https://yupi.icu">编程导航知识星球</a>
 */
@SpringBootTest
class BIApplicationTests {

    @Resource
    YuCongMingClient yuCongMingClient;

    @Test
    void test(){
        DevChatRequest devChatRequest=new DevChatRequest();
        devChatRequest.setModelId(1689272805134217218L);
        devChatRequest.setMessage("分析需求：网站的趋势及未来发展预估\n" +
                "图标类型：柱状图\n" +
                "原始数据：\n" +
                "日期,人数\n" +
                "1号,30\n" +
                "2号,40\n" +
                "3号,50\n" +
                "4号,10\n" +
                "5号,15\n" +
                "6号,100");
        BaseResponse<DevChatResponse> response = yuCongMingClient.doChat(devChatRequest);
        DevChatResponse data = response.getData();
        System.out.println(data.getContent());
    }
}
