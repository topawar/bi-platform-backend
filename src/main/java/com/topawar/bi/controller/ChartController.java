package com.topawar.bi.controller;

import cn.hutool.core.io.FileUtil;
import com.topawar.bi.common.ErrorCode;
import com.topawar.bi.common.ResultUtils;
import com.topawar.bi.exception.BusinessException;
import com.topawar.bi.exception.ThrowUtils;
import com.topawar.bi.model.dto.chart.GenRequest;
import com.topawar.bi.model.entity.Chart;
import com.topawar.bi.model.entity.User;
import com.topawar.bi.service.ChartService;
import com.topawar.bi.service.UserService;
import com.topawar.bi.utils.ExcelUtils;
import com.yupi.yucongming.dev.client.YuCongMingClient;
import com.yupi.yucongming.dev.common.BaseResponse;
import com.yupi.yucongming.dev.model.DevChatRequest;
import com.yupi.yucongming.dev.model.DevChatResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/chart")
public class ChartController {

    @Resource
    private YuCongMingClient yuCongMingClient;

    @Resource
    private ChartService chartService;

    @Resource
    private UserService userService;

    @PostMapping("/gen")
    public com.topawar.bi.common.BaseResponse<Chart> gen(GenRequest genRequest, @RequestPart MultipartFile multipartFile, HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (null == loginUser) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        //校验文件
        long size = multipartFile.getSize();
        final long TEN_SIZE = 1024 * 1024 * 10;
        ThrowUtils.throwIf(size>TEN_SIZE,ErrorCode.NOT_FOUND_ERROR,"文件不能超过10MB");
        String suffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        final List<String> SUFFIX_RULE_LIST = Arrays.asList("xls", "xlsx");
        ThrowUtils.throwIf(!SUFFIX_RULE_LIST.contains(suffix),ErrorCode.SYSTEM_ERROR,"文件格式必须xls/xlsx");
        StringBuilder builder = new StringBuilder();
        if (genRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        String readData = ExcelUtils.excelToCsv(multipartFile);
        String target = genRequest.getTarget();
        if (StringUtils.isBlank(target)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请输入分析目标");
        }
        builder.append("分析目标：").append(target).append("/n");
        String chartType = genRequest.getChartType();
        if (StringUtils.isNotBlank(chartType)) {
            builder.append("图表类型：").append(chartType).append("/n");
        }
        builder.append("原始数据：").append(readData);
        DevChatRequest devChatRequest = new DevChatRequest();
        devChatRequest.setModelId(1689272805134217218L);
        devChatRequest.setMessage(builder.toString());
        Chart chart = new Chart();
        chart.setGoal(target);
        chart.setName("分析图");
        chart.setChartData(readData);
        chart.setChartType(chartType);
        BaseResponse<DevChatResponse> chatResponse = yuCongMingClient.doChat(devChatRequest);
        String content = chatResponse.getData().getContent();
        String[] split = content.split("【【【【【");
        chart.setGenChart(split[1].trim());
        chart.setGenResult(split[2]);
        chart.setStatus("success");
        chart.setUserId(loginUser.getId());
        chartService.save(chart);
        return ResultUtils.success(chart);
    }

}
