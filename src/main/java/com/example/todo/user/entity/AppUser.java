package com.example.todo.user.entity;

import com.example.todo.common.entity.BaseEntity;
import com.example.todo.task.entity.Task;
import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Entity
@Table(name = "app_users")
public class AppUser extends BaseEntity implements UserDetails {

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

    // --- UserDetails methods ---

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Every user has ROLE_USER for now.
        // In a future lesson we will add a real Role entity.
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() { return password; }

    @Override
    public String getUsername() { return username; }

    // Spring Security 6+ has default implementations returning true for the
    // four boolean methods below, so we only override when we need custom logic.

    // --- Regular getters ---

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public List<Task> getTasks() { return tasks; }
    public void setPassword(String password) { this.password = password; }
}
