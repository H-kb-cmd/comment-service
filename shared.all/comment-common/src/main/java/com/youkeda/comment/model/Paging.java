package com.youkeda.comment.model;

import java.util.List;

/**
 * 分页结果包装
 *
 * 原本由私有 starter 提供，此处补齐。
 * 依据 CommentController 的用法反推：
 *   new Paging<>(page.getPageNum(), page.getPageSize(), page.getPages(),
 *                page.getTotal(), page.getResult())
 *
 * 字段对应 PageHelper 的 Page 对象：
 *   pageNum 当前页码 / pageSize 每页条数 / pages 总页数 / total 总记录数 / data 当前页数据
 */
public class Paging<T> {

    /** 当前页码 */
    private int pageNum;

    /** 每页条数 */
    private int pageSize;

    /** 总页数 */
    private int pages;

    /** 总记录数 */
    private long total;

    /** 当前页数据 */
    private List<T> data;

    public Paging() {
    }

    public Paging(int pageNum, int pageSize, int pages, long total, List<T> data) {
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.pages = pages;
        this.total = total;
        this.data = data;
    }

    public int getPageNum() {
        return pageNum;
    }

    public void setPageNum(int pageNum) {
        this.pageNum = pageNum;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getPages() {
        return pages;
    }

    public void setPages(int pages) {
        this.pages = pages;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }
}
