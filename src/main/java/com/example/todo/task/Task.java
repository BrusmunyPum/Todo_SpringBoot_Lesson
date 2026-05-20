//package com.example.todo.task;

// before database
//public class Task {
//    private Long id;
//    private String title;
//    private boolean completed;
//
//    // constructor
//    public Task(Long id, String title, boolean completed){
//        this.id = id;
//        this.title = title;
//        this.completed = completed;
//    }
//
//    // overload get and set method
//    public Long getId(){
//        return id;
//    }
//    public String getTitle(){
//        return title;
//    }
//    public boolean isCompleted(){
//        return completed;
//    }
//    public void setId(Long id){
//        this.id = id;
//    }
//    public void setTitle(String title){
//        this.title = title;
//    }
//    public void setCompleted(boolean completed){
//        this.completed = completed;
//    }
//
//}


// after database

//import jakarta.persistence.Column;
//import jakarta.persistence.Entity;
//import jakarta.persistence.EntityListeners;
//import jakarta.persistence.GeneratedValue;
//import jakarta.persistence.GenerationType;
//import jakarta.persistence.Id;
//import jakarta.persistence.Table;
//import org.springframework.data.annotation.CreatedDate;
//import org.springframework.data.annotation.LastModifiedDate;
//import org.springframework.data.jpa.domain.support.AuditingEntityListener;
//
//import java.time.Instant;
//
//@Entity
//@Table(name = "tasks")
//@EntityListeners(AuditingEntityListener.class)
//public class Task {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    private Long id;
//
//    @Column(nullable = false, length = 100)
//    private String title;
//
//    @Column(nullable = false)
//    private boolean completed;
//
//    @CreatedDate
//    @Column(nullable = false, updatable = false)
//    private Instant createdAt;
//
//    @LastModifiedDate
//    @Column(nullable = false)
//    private Instant updatedAt;
//
//    protected Task() {
//    }
//
//    public Task(String title, boolean completed) {
//        this.title = title;
//        this.completed = completed;
//    }
//
//    public Long getId() {
//        return id;
//    }
//
//    public String getTitle() {
//        return title;
//    }
//
//    public boolean isCompleted() {
//        return completed;
//    }
//
//    public Instant getCreatedAt() {
//        return createdAt;
//    }
//
//    public Instant getUpdatedAt() {
//        return updatedAt;
//    }
//
//    public void setTitle(String title) {
//        this.title = title;
//    }
//
//    public void setCompleted(boolean completed) {
//        this.completed = completed;
//    }
//
//
//
//}


package com.example.todo.task;

import com.example.todo.comment.TaskComment;
import com.example.todo.user.AppUser;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tasks")
@EntityListeners(AuditingEntityListener.class)
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false)
    private boolean completed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskPriority priority = TaskPriority.MEDIUM;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TaskComment> comments = new ArrayList<>();

    protected Task() {
    }

    public Task(String title, boolean completed, TaskPriority priority, LocalDate dueDate, AppUser user) {
        this.title = title;
        this.completed = completed;
        this.priority = priority;
        this.dueDate = dueDate;
        this.user = user;
    }

    public AppUser getUser() {
        return user;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public boolean isCompleted() {
        return completed;
    }

    public TaskPriority getPriority() {
        return priority;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<TaskComment> getComments() {
        return comments;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public void setPriority(TaskPriority priority) {
        this.priority = priority;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }





}


