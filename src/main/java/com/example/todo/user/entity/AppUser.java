package com.example.todo.user.entity;

import com.example.todo.common.entity.BaseEntity;
import com.example.todo.task.entity.Task;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "app_users")
public class AppUser extends BaseEntity { // Extends BaseEntity!

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Column(nullable = false)
    private String password;

    @OneToMany(mappedBy = "user", fetch = FetchType.LAZY)
    private List<Task> tasks = new ArrayList<>();

    protected AppUser() {}

    public AppUser(String username, String email, String password) {
        this.username = username;
        this.email = email;
        this.password = password;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public List<Task> getTasks() { return tasks; }
    public void setPassword(String password) { this.password = password; }
}