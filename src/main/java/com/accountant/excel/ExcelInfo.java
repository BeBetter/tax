package com.accountant.excel;

import java.util.List;

/**
 * @author xushengguo-xy
 * @date 2019/5/3 20:51
 */
public class ExcelInfo {
    private String sheetName;
    private List<String> headers;
    private List<List<Object>> data;

    public ExcelInfo(String sheetName,
                     List<String> headers,
                     List<List<Object>> data) {
        this.sheetName = sheetName;
        this.headers = headers;
        this.data = data;
    }

    public String getSheetName() {
        return sheetName;
    }

    public void setSheetName(String sheetName) {
        this.sheetName = sheetName;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public void setHeaders(List<String> headers) {
        this.headers = headers;
    }

    public List<List<Object>> getData() {
        return data;
    }

    public void setData(List<List<Object>> data) {
        this.data = data;
    }
}
