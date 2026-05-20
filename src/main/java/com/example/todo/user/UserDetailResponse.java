package com.example.todo.user;

import java.time.Instant;

public class UserDetailResponse {
    private Long id;
    private String username;
    private String email;
    private Long taskCount;
    private Instant createdAt;
    private Instant updatedAt;

    public UserDetailResponse(Long id,String username,String email,Long taskCount,Instant createdAt,Instant updatedAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.taskCount = taskCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId(){
        return id;
    }

    public String getUsername(){
        return username;
    }
    public String getEmail(){
        return email;
    }
    public Long getTaskCount(){
        return taskCount;
    }
    public Instant getCreatedAt(){
        return createdAt;
    }
    public Instant getUpdatedAtt(){
        return updatedAt;
    }


}
