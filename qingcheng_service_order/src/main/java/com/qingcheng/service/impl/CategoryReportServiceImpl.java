package com.qingcheng.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.qingcheng.dao.CategoryReportMapper;
import com.qingcheng.pojo.order.CategoryReport;
import com.qingcheng.service.order.CategoryReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service(interfaceClass = CategoryReportService.class)
public class CategoryReportServiceImpl implements CategoryReportService {

    @Autowired
    private CategoryReportMapper categoryReportMapper;

    @Override
    public List<CategoryReport> categoryReport(LocalDate date) {
        List<CategoryReport> categoryReports = categoryReportMapper.categoryReport(date);
        return categoryReports;
    }

    /**
     * 对昨天的数据进行统计
     */
    @Transactional
    @Override
    public void creataData() {
        //1.首先得到昨天的日期
        LocalDate localDate = LocalDate.now().minusDays(1);
        //2.调用数据统计方法进行查询
        List<CategoryReport> categoryReports = categoryReportMapper.categoryReport(localDate);
        //3.将查询到的数据一条一条的记录到categoryReport数据库表中
        for (CategoryReport categoryReport : categoryReports) {
            categoryReportMapper.insertSelective(categoryReport);
        }
    }

    @Override
    public List<Map> categoryCount1(String date1, String date2) {
        List<Map> mapList = categoryReportMapper.category1Count(date1, date2);
        //System.out.println(mapList);
        return mapList;
    }
}
