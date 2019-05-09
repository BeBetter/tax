package com.accountant.tax;

import com.accountant.excel.Excel;
import com.accountant.excel.ExcelInfo;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xushengguo-xy
 */
public class FreeTax {
    private static Logger logger = LoggerFactory.getLogger(FreeTax.class);

    //主程序入口
    public static void main(String[] args) throws ParseException {
        String filepath = "D:\\git_workspace\\tax\\src\\data\\2018_new.xls";
        Excel excel = new Excel();
        Workbook wb = excel.read(filepath);

        // 对读取Excel表格内容测试
        //sheet div数据
        List<List<Object>> div = excel.content(wb, "2018DIV");
        //sheet end数据
        List<List<Object>> end = endETL(excel.content(wb, "2016END"));
        //sheet transAmount 数据
        List<List<Object>> transAmount = excel.content(wb, "TRANSAMOUNT");
        transAmount.addAll(end);

        //计算
        List<List<Object>> resList = new ArrayList<>();
        preCompute(resList, div, transAmount);

        //写文件
        List<String> titles = new ArrayList<>();
        titles.add("DB");
        titles.add("证券代码");
        titles.add("证券名称");
        titles.add("凭证日期");
        titles.add("汇总");
        titles.add("分红总股数");
        titles.add("免税股数");
        ExcelInfo info = new ExcelInfo("免税计算", titles, resList);

        excel.write(info, "D:\\git_workspace\\tax\\src\\data\\free_tax_result_new_1.xls");
    }

    private static void preCompute(List<List<Object>> res,
                                   List<List<Object>> div,
                                   List<List<Object>> transAmount) throws ParseException {
        //处理
        for (List<Object> list : div) {
            String db = ((String) list.get(0)).trim();
            String code = ((String) list.get(1)).trim();
            String name = ((String) list.get(2)).trim();

            List<List<Object>> filterList = transAmount.stream().filter(trans -> {
                String mapDb = ((String) trans.get(0)).trim();
                String mapCode = ((String) trans.get(1)).trim();
                String mapName = ((String) trans.get(2)).trim();
                return db.equals(mapDb) && code.equals(mapCode) && name.equals(mapName);
            }).sorted(Comparator.comparing(key -> (Date) key.get(3))).collect(Collectors.toList());

            if (db.equals("阳光团险") && name.equals("长城汽车")) {
                List<Object> result = computeFreeTax(list, filterList);
                res.add(result);
            }
        }
    }


    private static List<Object> computeFreeTax(List<Object> div,
                                               List<List<Object>> amounts) throws ParseException {
        //分红日期
        Date date = null;
        try {
            date = (Date) div.get(3);
        } catch (ClassCastException ex) {
            System.out.println("div: " + div);
        }
        //分红之前的交易数据
        List<List<Object>> beforeAmounts = new ArrayList<>();
        //分红之后的交易数据
        List<List<Object>> afterAmounts = new ArrayList<>();

        for (List<Object> temp : amounts) {
            Date tempDate = (Date) temp.get(3);
            if (tempDate.before(date)) {
                beforeAmounts(beforeAmounts, temp);
            } else if (tempDate.equals(date)) {
                String category = (String) temp.get(4);
                if (category.equals("卖出")) {
                    beforeAmounts(beforeAmounts, temp);
                }
            } else {
                afterAmounts(afterAmounts, temp);
            }
        }
        List<Double> res = beforeAmounts.stream().map(s -> (Double) s.get(5)).collect(Collectors.toList());
        double sum = 0.0;
        for (double d : res) {
            sum += d;
        }
        div.add(5, sum);


        //计算一年分红
        for (List<Object> temp : afterAmounts) {
            profitTax(beforeAmounts, temp, div);
        }

        //分红之前剩下截止时间处理
        Calendar cal = Calendar.getInstance();
        Date lastDay = (new SimpleDateFormat("yyyyMMdd")).parse("20181231");
        cal.setTime(lastDay);
        cal.add(Calendar.YEAR, -1);
        lastDay = cal.getTime();
        Double profit = 0.0;
        for (List<Object> beforeMap : beforeAmounts) {
            Date bDate = (Date) beforeMap.get(3);
            if (bDate.before(lastDay)) {
                profit += (Double) beforeMap.get(5);
            }
        }

        if (div.size() == 7) {
            div.set(6, (double) div.get(6) + profit);
        } else {
            div.add(profit);
        }
        return div;
    }

    //计算满一年的分红
    private static void profitTax(List<List<Object>> beforeAmounts,
                                  List<Object> amount,
                                  List<Object> div) {
        Calendar cal = Calendar.getInstance();
        Double profit = 0.0;
        Date saleDate = (Date) amount.get(3);
        Double stockNum = (Double) amount.get(5) * -1;
        for (int i = 0; i < beforeAmounts.size(); i++) {
            Double num = (Double) beforeAmounts.get(i).get(5);
            if (num == 0.0) {
                continue;
            }
            double stockNum1 = num + stockNum;
            Date tDate = (Date) beforeAmounts.get(i).get(3);
            cal.setTime(tDate);
            cal.add(Calendar.YEAR, 1);
            if (stockNum1 < 0) { //当前没有
                beforeAmounts.get(i).set(5, 0.0);
                if (cal.getTime().before(saleDate)) {
                    profit += num;
                }
                stockNum = stockNum1;
            } else {
                beforeAmounts.get(i).set(5, stockNum1);
                if (cal.getTime().before(saleDate)) {
                    profit += stockNum * -1;
                }
                break;
            }
        }

        if (div.size() == 7) {
            div.set(6, (double) div.get(6) + profit);
        } else {
            div.add(profit);
        }
    }

    //分红后数据处理
    private static void afterAmounts(List<List<Object>> afterAmounts,
                                     List<Object> amount) {
        String category = (String) amount.get(4);
        if (category.equals("卖出")) {
            afterAmounts.add(amount);
        }
    }

    //分红之前计算
    private static void beforeAmounts(List<List<Object>> beforeAmounts,
                                      List<Object> amount) {
        String category = (String) amount.get(4);
        Double stockNum = (Double) amount.get(5) * -1;
        if (category.equals("买入")) {
            beforeAmounts.add(amount);
        } else { //卖出
            for (int i = 0; i < beforeAmounts.size(); i++) {
                Double num = (Double) beforeAmounts.get(i).get(5);
                if (num == 0.0) {
                    continue;
                }
                stockNum = num + stockNum;
                if (stockNum < 0) {
                    beforeAmounts.get(i).set(5, 0.0);
                } else {
                    beforeAmounts.get(i).set(5, stockNum);
                    break;
                }
            }
        }
    }

    private static List<List<Object>> endETL(List<List<Object>> list) {
        return list.stream()
                .filter(t1List -> {
                    Object obj = t1List.get(4);
                    return !(obj instanceof Double) || !(((Double) obj) < 0.0);
                }).peek(t2List -> {
                    Object obj = t2List.get(4);
                    if (obj instanceof String) {
                        t2List.set(4, 0.0);
                    }
                    t2List.add(4, "买入");
                }).collect(Collectors.toList());
    }
}
