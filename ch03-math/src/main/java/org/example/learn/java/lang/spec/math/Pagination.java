package org.example.learn.java.lang.spec.math;



public class Pagination {
    /**
     * 根据余数是否为零来判断是否能够整除,来达到上取整(ceiling)
     *
     */
    public static int calcPageNos0(int totalRecords, int pageSize) {
        int pageNos = -1;
        // 手工实现向上取整
        pageNos = (totalRecords / pageSize) + (totalRecords % pageSize == 0 ? 0 : 1);

        return pageNos;
    }

    /**
     * 使用自带的Math.ceil方法实现向上取整
     */
    public static int calcPageNos1(int totalRecords, int pageSize) {
        int pageNos = -1;
        pageNos = (int) Math.ceil((double)totalRecords / (double)pageSize);

        return pageNos;
    }

    /**
     * 巧解法
     *
     * 解题思路: 令 totalRecords = p * pageSize + q,其中 p和q都是非负整数,且0<=q<pageSize
     *
     * (totalRecords + (pageSize - 1)) / pageSize
     * =ceil((double)(p *pageSize+q+pageSize-1)/(double)pageSize)
     * =ceil(p+1+((double)(q-1)/(double)pageSize))
     *   当q=0时,((double)(q-1)/(double)pageSize)为负数,此时ceil(p+1+((double)(q-1)/(double)pageSize))=p
     *   当q>0时,即q>=1,((double)(q-1)/(double)pageSize)为正数,此时ceil(p+1+((double)(q-1)/(double)pageSize))=p+1
     * 所以(totalRecords + (pageSize - 1)) / pageSize等价于向上取整
     *    即(totalRecords + (pageSize - 1)) / pageSize等于Math.ceil((double)totalRecords/(double)pageSize)
     */
    public static int calcPageNos2(int totalRecords, int pageSize) {
        int pageNos = -1;
        pageNos = (totalRecords + (pageSize - 1)) / pageSize;

        return pageNos;
    }

    /**
     * 当前内的数据
     * @param startPageNo 1-based
     * @param endPageNo 1-based
     * @param pageSize page size
     * @param totalRecords total records
     * @return range of data 0-based index
     */
    public static PagingRange<Object> calcRange(int startPageNo, int endPageNo, int pageSize, int totalRecords) {
        int totalPageNos = calcPageNos1(totalRecords, pageSize);
        if (startPageNo < 1) {
            throw new IllegalArgumentException("startPageNo less than min page number");
        }

        if (endPageNo > totalPageNos) {
            throw new IllegalArgumentException("endPageNo greater than max page number");
        }

        int maxIndex = totalRecords - 1;
        int startIndex = (startPageNo - 1) * pageSize;
        int endIndex = Math.min(endPageNo * pageSize, maxIndex);

        return new PagingRange<Object>(startIndex, endIndex, true, true);
    }

    public static PagingRange<Object> safeCalcRange(int startPageNo, int endPageNo, int pageSize, int totalRecords) {
        int totalPageNos = calcPageNos1(totalRecords, pageSize);
        endPageNo = Math.min(endPageNo, totalPageNos);
        startPageNo = Math.max(startPageNo, 1);

        int startIndex = (startPageNo - 1) * pageSize;
        int endIndex = Math.min(endPageNo * pageSize, totalRecords);

        return new PagingRange<Object>(startIndex, endIndex, true, true);
    }
}
