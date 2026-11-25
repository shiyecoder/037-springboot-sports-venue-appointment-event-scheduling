package com.grad.svs.common;

import java.util.List;

/**
 * 分页返回：总数、页码、每页条数与列表，前端据此渲染分页器。
 */
public class PageResult<T> {

    private long total;
    private long page;
    private long size;
    private List<T> list;

    public PageResult() {
    }

    public PageResult(long total, long page, long size, List<T> list) {
        this.total = total;
        this.page = page;
        this.size = size;
        this.list = list;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public long getPage() {
        return page;
    }

    public void setPage(long page) {
        this.page = page;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public List<T> getList() {
        return list;
    }

    public void setList(List<T> list) {
        this.list = list;
    }
}
