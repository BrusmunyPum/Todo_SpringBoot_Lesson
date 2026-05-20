package com.example.todo.task.dto.response;

import java.util.List;

public class TaskPageResponse {
    private List<TaskResponse> content;
    private int page;
    private int size;
    private Long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;

    public TaskPageResponse(List<TaskResponse> content, int page, int size, Long totalElements, int totalPages, boolean first, boolean last) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.first = first;
        this.last = last;
    }

    public List<TaskResponse> getContent() {
        return content;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public Long getTotalElements() {
        return totalElements;
    }

    public boolean isFirst() {
        return first;
    }

    public boolean isLast() {
        return last;
    }


}
