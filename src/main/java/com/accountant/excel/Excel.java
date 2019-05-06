package com.accountant.excel;

import org.apache.commons.io.FileUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author xushengguo-xy
 * @date 2019/5/3 20:44
 */
public class Excel {
    private static Logger logger = LoggerFactory.getLogger(Excel.class);

    /**
     * 读取excel.
     *
     * @param filePath 文件路径
     * @return 返回excel
     */
    public Workbook read(String filePath) {
        Workbook wb = null;
        if (filePath == null) {
            return null;
        }

        String ext = filePath.substring(filePath.lastIndexOf("."));
        try {
            InputStream is = new FileInputStream(filePath);
            if (".xls".equals(ext)) {
                wb = new HSSFWorkbook(is);
            } else if (".xlsx".equals(ext)) {
                wb = new XSSFWorkbook(is);
            }
        } catch (IOException ex) {
            logger.error("IOException", ex);
        }
        return wb;
    }

    //将数据写进excel中
    public void write(ExcelInfo info, String filePath) {
        //创建excel工作簿
        HSSFWorkbook workbook = new HSSFWorkbook();
        //创建工作表sheet
        HSSFSheet sheet = workbook.createSheet(info.getSheetName());
        //创建第一行
        HSSFRow row = sheet.createRow(0);
        HSSFCell cell;

        //插入第一行数据的表头
        for (int i = 0; i < info.getHeaders().size(); i++) {
            cell = row.createCell(i);
            cell.setCellValue(info.getHeaders().get(i));
        }
        //写入数据
        for (int i = 1; i <= info.getData().size(); i++) {
            HSSFRow nRow = sheet.createRow(i);
            List<Object> objs = info.getData().get(i - 1);
            for (int j = 0; j < info.getHeaders().size(); j++) {
                cell = nRow.createCell(j);
                Object obj = objs.get(j);
                if (obj instanceof String) {
                    cell.setCellValue((String) obj);
                } else if (obj instanceof Double) {
                    cell.setCellValue((double) obj);
                } else if (obj instanceof Date) {
                    cell.setCellValue((Date) obj);
                } else {
                    cell.setCellValue(obj.toString());
                }
            }
        }
        //创建excel文件
        File file = new File(filePath);
        try (FileOutputStream stream = FileUtils.openOutputStream(file)) {
            //将excel写入
            workbook.write(stream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 读取表头.
     *
     * @param wb workbook
     * @return string[]
     */
    public List<String> title(Workbook wb, String sheetName) {
        if (wb == null) {
            throw new NullPointerException("workbook 是空对象！");
        }

        Sheet sheet = wb.getSheet(sheetName);
        Row row = sheet.getRow(0);
        int columnNum = row.getPhysicalNumberOfCells();

        List<String> titles = new ArrayList<>();
        for (int i = 0; i < columnNum; i++) {
            titles.add(row.getCell(i).getStringCellValue());
        }
        return titles;
    }

    /**
     * 读取excel内容.
     *
     * @param wb workbook
     * @return map
     */
    public List<List<Object>> content(Workbook wb, String sheetName) {
        if (wb == null) {
            throw new NullPointerException("workbook 是空对象！");
        }

        List<List<Object>> list = new ArrayList<>();

        Sheet sheet = wb.getSheet(sheetName);
        int rowNum = sheet.getLastRowNum();
        Row row;
        for (int i = 1; i <= rowNum; i++) {
            row = sheet.getRow(i);
            int colNum = row.getLastCellNum();
            List<Object> objects = new ArrayList<>();
            for (int j = 0; j < colNum; j++) {
                Object obj = getCellFormatValue(row.getCell(j));
                objects.add(obj);
            }
            list.add(objects);
        }
        return list;
    }

    /**
     * 读取单元格内容.
     *
     * @param cell 单元格对象
     * @return 返回读后的值
     */
    private Object getCellFormatValue(Cell cell) {
        Object cellvalue = "";
        if (cell != null) {
            // 判断当前Cell的Type
            switch (cell.getCellTypeEnum()) {
                case NUMERIC:// 如果当前Cell的Type为NUMERIC
                    if (DateUtil.isCellDateFormatted(cell)) {
                        cellvalue = cell.getDateCellValue();
                    } else {
                        cellvalue = cell.getNumericCellValue();
                    }
                    break;
                case FORMULA:
                    cellvalue = cell.getCellFormula();
                    break;
                case STRING:// 如果当前Cell的Type为STRING
                    cellvalue = cell.getStringCellValue();
                    break;
                default:// 默认的Cell值
                    break;
            }
        }
        return cellvalue;
    }
}
