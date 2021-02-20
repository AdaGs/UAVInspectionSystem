package com.gjdw.stserver.config;

import java.util.ArrayList;
import java.util.List;

public class PageResult <T>{


    private int pageSize=10; // 每页显示多少条记录

    private int currentPage=1; //当前第几页数据

    private int total=0; // 一共多少条记录

    private int totalPage=0; // 一共多少页

    private List<T> dataList; //要显示的数据

    public PageResult(int pageNum, int pageSize, List<T> sourceList) {
        if (sourceList == null || sourceList.isEmpty()) {
            this.dataList=new ArrayList<>();
            return ;
        }

        // 总记录条数
        this.total = sourceList.size();

        // 每页显示多少条记录
        this.pageSize = pageSize;

        //获取总页数
        this.totalPage = this.total / this.pageSize + 1;
//        if (this.total % this.pageSize != 0) {
//            this.totalPage = this.totalPage + 1;
//        }

        // 当前第几页数据
        this.currentPage = this.totalPage < pageNum ? this.totalPage : pageNum;

        // 起始索引
        int fromIndex = this.pageSize * (this.currentPage - 1);

        // 结束索引
        int toIndex = this.pageSize * this.currentPage > this.total ? this.total : this.pageSize * this.currentPage;

        this.dataList = sourceList.subList(fromIndex, toIndex);
    }

    private PageResult() {
    }

    public PageResult(int pageSize, int currentPage, int totalRecord, int totalPage,
                    List<T> dataList) {
        super();
        this.pageSize = pageSize;
        this.currentPage = currentPage;
        this.total = totalRecord;
        this.totalPage = totalPage;
        this.dataList = dataList;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getTotalRecord() {
        return total;
    }

    public void setTotalRecord(int totalRecord) {
        this.total = totalRecord;
    }

    public int getTotalPage() {
        return totalPage;
    }

    public void setTotalPage(int totalPage) {
        this.totalPage = totalPage;
    }

    public List<T> getDataList() {
        return dataList;
    }

    public void setDataList(List<T> dataList) {
        this.dataList = dataList;
    }


}
