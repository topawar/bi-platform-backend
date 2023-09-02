package com.topawar.bi.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.topawar.bi.mapper.ChartMapper;
import com.topawar.bi.model.entity.Chart;
import com.topawar.bi.service.ChartService;
import org.springframework.stereotype.Service;

/**
* @author topawar
* @description 针对表【chart(图表信息表)】的数据库操作Service实现
* @createDate 2023-08-07 16:07:46
*/
@Service
public class ChartServiceImpl extends ServiceImpl<ChartMapper, Chart>
    implements ChartService {

}




