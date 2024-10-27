package org.example.learn.java.lang.spec.math;

import org.junit.Test;

/**
 * 分页计算
 */
public class PagingTest {

    /**
     * 计算页数
     */
    @Test
    public void testPageNos() {
        int totalRecords = 11;
        int pageSize = 3;
        int pageNos = -1;

        pageNos = Pagination.calcPageNos0(totalRecords, pageSize);
        System.out.println("pageNos = " + pageNos);

        pageNos = Pagination.calcPageNos1(totalRecords, pageSize);
        System.out.println("pageNos = " + pageNos);

        pageNos = Pagination.calcPageNos2(totalRecords, pageSize);
        System.out.println("pageNos = " + pageNos);
    }

    /**
     * 计算起始结束区间内的数据
     */
    @Test
    public void testPagingRange() {
        int totalRecords = 11;
        int pageSize = 3;

        PagingRange<Object> pagingRange;

        // 当前页的内容,页号是1-based
        pagingRange = Pagination.calcRange(1, 1, pageSize, totalRecords);
        System.out.println("pagingRange = " + pagingRange);

        // 区间页的内容,页号是1-based
        pagingRange = Pagination.calcRange(1, 2, pageSize, totalRecords);
        System.out.println("pagingRange = " + pagingRange);

        pagingRange = Pagination.calcRange(4, 4, pageSize, totalRecords);
        System.out.println("pagingRange = " + pagingRange);

    }
}
