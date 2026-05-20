package com.example.todo.comment.entity;

import com.example.todo.common.entity.BaseEntity;
import com.example.todo.task.entity.Task;
import jakarta.persistence.*;

@Entity
@Table(name = "task_comments")
public class TaskComment extends BaseEntity { // Extends BaseEntity

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 500)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    protected TaskComment() {}

    public TaskComment(String content, Task task) {
        this.content = content;
        this.task = task;
    }

    public Long getId() { return id; }
    public String getContent() { return content; }
    public Task getTask() { return task; }
    public void setContent(String content) { this.content = content; }
}