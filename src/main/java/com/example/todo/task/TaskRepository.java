//package com.example.todo.task;
//
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//
//import java.util.List;
//
//public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {
//
//    // N+1 task query
//    @Query(
//            value = """
//                    SELECT t FROM Task t
//                    LEFT JOIN FETCH t.user
//                    """,
//            countQuery = """
//                    SELECT COUNT(t) FROM Task t
//                    """
//    )
//    Page<Task> findAllWithUser(Pageable pageable);
//
//    // for user
//    @Query(
//            value = """
//                SELECT t FROM Task t
//                LEFT JOIN FETCH t.user
//                WHERE t.user.id = :userId
//                """,
//            countQuery = """
//                SELECT COUNT(t) FROM Task t
//                WHERE t.user.id = :userId
//                """
//    )
//    Page<Task> findByUserIdWithUser(@Param("userId") Long userId, Pageable pageable);
//
//
//    Page<Task> findByCompleted(boolean completed, Pageable pageable);
//
//    Page<Task> findByUserId(Long userId, Pageable pageable);
//
//    Page<Task> findByUserIdAndCompleted(Long userId, boolean completed, Pageable pageable);
//
//    List<Task> findByTitleContainingIgnoreCase(String title);
//}

package com.example.todo.task;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    @EntityGraph(attributePaths = "user")
    Page<Task> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<Task> findByCompleted(boolean completed, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<Task> findByUserId(Long userId, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    Page<Task> findByUserIdAndCompleted(Long userId, boolean completed, Pageable pageable);

    Long countByUserId(Long userId);

    List<Task> findByTitleContainingIgnoreCase(String title);
}