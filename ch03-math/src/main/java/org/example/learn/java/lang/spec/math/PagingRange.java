package org.example.learn.java.lang.spec.math;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.util.List;

/**
 * 数据的索引是0-based
 * @param <T> 数据的类型
 */
public class PagingRange<T> {

    // 数据项的索引号
    private final int startIndex;
    private final int endIndex;
    final boolean includeStartIndex;
    final boolean includeEndIndex;

    private List<T> data;

    public PagingRange(int start, int end, boolean includeStart, boolean includeEnd) {
        this.startIndex = start;
        this.endIndex = end;
        this.includeStartIndex = includeStart;
        this.includeEndIndex = includeEnd;
    }

    public List<T> getData() {
        return data;
    }

    public void setData(List<T> data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("startIndex", startIndex)
                .append("endIndex", endIndex)
                .append("includeStartIndex", includeStartIndex)
                .append("includeEndIndex", includeEndIndex)
                .append("data", data)
                .toString();
    }
}
