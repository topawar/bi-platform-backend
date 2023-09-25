package com.topawar.bi.controller;

import cn.hutool.core.io.FileUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.topawar.bi.common.ErrorCode;
import com.topawar.bi.common.PageRequest;
import com.topawar.bi.common.ResultUtils;
import com.topawar.bi.constant.BiConstant;
import com.topawar.bi.exception.BusinessException;
import com.topawar.bi.exception.ThrowUtils;
import com.topawar.bi.model.dto.chart.GenRequest;
import com.topawar.bi.model.entity.Chart;
import com.topawar.bi.model.entity.User;
import com.topawar.bi.model.vo.ChartVo;
import com.topawar.bi.service.ChartService;
import com.topawar.bi.service.UserService;
import com.topawar.bi.utils.ExcelUtils;
import com.yupi.yucongming.dev.client.YuCongMingClient;
import com.yupi.yucongming.dev.common.BaseResponse;
import com.yupi.yucongming.dev.model.DevChatRequest;
import com.yupi.yucongming.dev.model.DevChatResponse;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.weaver.ast.Var;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;

@RestController
@RequestMapping("/chart")
public class ChartController {

    @Resource
    private YuCongMingClient yuCongMingClient;

    @Resource
    private ChartService chartService;

    @Resource
    private UserService userService;

    @Resource
    private ThreadPoolExecutor threadPoolExecutor;

    @PostMapping("/gen")
    public com.topawar.bi.common.BaseResponse<ChartVo> gen(GenRequest genRequest, @RequestPart MultipartFile multipartFile, HttpServletRequest request) {
        User loginUser = isLogin(request);
        //校验文件
        long size = multipartFile.getSize();
        final long TEN_SIZE = 1024 * 1024 * 10;
        ThrowUtils.throwIf(size > TEN_SIZE, ErrorCode.NOT_FOUND_ERROR, "文件不能超过10MB");
        String suffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        final List<String> SUFFIX_RULE_LIST = Arrays.asList("xls", "xlsx");
        ThrowUtils.throwIf(!SUFFIX_RULE_LIST.contains(suffix), ErrorCode.SYSTEM_ERROR, "文件格式必须xls/xlsx");
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
        devChatRequest.setModelId(BiConstant.BI_MODEL_ID);
        devChatRequest.setMessage(builder.toString());
        Chart chart = new Chart();
        chart.setGoal(target);
        chart.setName("分析图");
        chart.setChartData(readData);
        chart.setChartType(chartType);
        BaseResponse<DevChatResponse> chatResponse = yuCongMingClient.doChat(devChatRequest);
        String content = chatResponse.getData().getContent();
        String[] split = content.split("【【【【【");
        String genChart = split[1].trim();
        String genResult = split[2].trim();
        chart.setGenChart(genChart);
        chart.setGenResult(genResult);
        chart.setStatus("success");
        chart.setUserId(loginUser.getId());
        chartService.save(chart);
        ChartVo chartVo = new ChartVo();
        chartVo.setGenChart(genChart);
        chartVo.setGenResult(genResult);
        return ResultUtils.success(chartVo);
    }


    @PostMapping("/gen/async")
    public com.topawar.bi.common.BaseResponse<ChartVo> genAsync(GenRequest genRequest, @RequestPart MultipartFile multipartFile, HttpServletRequest request) {
        User loginUser = isLogin(request);
        //校验文件
        long size = multipartFile.getSize();
        final long TEN_SIZE = 1024 * 1024 * 10;
        ThrowUtils.throwIf(size > TEN_SIZE, ErrorCode.NOT_FOUND_ERROR, "文件不能超过10MB");
        String suffix = FileUtil.getSuffix(multipartFile.getOriginalFilename());
        final List<String> SUFFIX_RULE_LIST = Arrays.asList("xls", "xlsx");
        ThrowUtils.throwIf(!SUFFIX_RULE_LIST.contains(suffix), ErrorCode.SYSTEM_ERROR, "文件格式必须xls/xlsx");
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
        devChatRequest.setModelId(BiConstant.BI_MODEL_ID);
        devChatRequest.setMessage(builder.toString());
        Chart chart = new Chart();
        chart.setGoal(target);
        chart.setName(genRequest.getName());
        chart.setChartData(readData);
        chart.setChartType(chartType);
        chart.setUserId(loginUser.getId());
        chart.setStatus("running");
        boolean save = chartService.save(chart);
        Long id = chart.getId();
        ChartVo chartVo = new ChartVo();
        if (save) {
            CompletableFuture.runAsync(() -> {
                Chart updateChart = new Chart();
                updateChart.setId(id);
                BaseResponse<DevChatResponse> chatResponse = yuCongMingClient.doChat(devChatRequest);
                if (0 == chatResponse.getCode()) {
                    updateChart.setStatus("error");
                    updateChart.setExecMessage("Ai服务异常");
                    chartService.updateById(updateChart);
                }
                String content = chatResponse.getData().getContent();
                String[] split = content.split("【【【【【");
                String genChart = split[1].trim();
                String genResult = split[2].trim();
                updateChart.setGenChart(genChart);
                updateChart.setGenResult(genResult);
                updateChart.setStatus("success");
                chartService.updateById(updateChart);
                chartVo.setGenChart(genChart);
                chartVo.setGenResult(genResult);
            }, threadPoolExecutor);
        } else {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        return ResultUtils.success(chartVo);
    }


    public User isLogin(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        if (null == loginUser) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return loginUser;
    }

    @GetMapping("/list/page")
    public com.topawar.bi.common.BaseResponse<Page<Chart>> listByPage(PageRequest pageRequest) {
        long pageSize = pageRequest.getPageSize();
        long current = pageRequest.getCurrent();
        Page<Chart> chartPage = new Page<>(current, pageSize);
        QueryWrapper<Chart> queryWrapper = new QueryWrapper<>();
        queryWrapper.orderByDesc("createTime");
        Page<Chart> page = chartService.page(chartPage, queryWrapper);
        return ResultUtils.success(page);
    }

}
