package de.softwaretesting.studyconnect.models;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Task entity following the specification in docs/spec.md.
 * - supports multiple assignees
 * - supports tags as an ElementCollection
 * - includes helper methods for entity management
 */
@Entity
@Table(name = "tasks")
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    private Priority priority = Priority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.OPEN;

    @Column(name = "category")
    private String category;

    @ElementCollection
    @CollectionTable(name = "task_tags")
    @Column(name = "tag")
    private Set<String> tags = new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @ManyToMany
    @JoinTable(name = "task_assignees",
            joinColumns = @JoinColumn(name = "task_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<User> assignees = new HashSet<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @ManyToOne
    @JoinColumn(name = "group_id")
    private Group group;

    // Helper methods

    public void addAssignee(User user) {
        if (user == null) return;
        assignees.add(user);
    }

    public void removeAssignee(User user) {
        if (user == null) return;
        assignees.remove(user);
    }

    public void addTag(String tag) {
        if (tag == null || tag.isBlank()) return;
        tags.add(tag.trim());
    }

    public void removeTag(String tag) {
        if (tag == null) return;
        tags.remove(tag.trim());
    }

    public void markComplete() {
        this.status = Status.COMPLETED;
        this.updatedAt = LocalDateTime.now();
    }

    public void setInProgress() {
        this.status = Status.IN_PROGRESS;
        this.onUpdate();
    }

    public boolean isOverdue() {
        if (dueDate == null) return false;
        return status != Status.COMPLETED && dueDate.isBefore(LocalDateTime.now());
    }

    @PrePersist
    void onCreate() {
        if (this.createdAt == null) this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Inner enums for Priority and Status to keep them scoped with Task
    public static enum Priority {
        LOW,
        MEDIUM,
        HIGH
    }

    public static enum Status {
        OPEN,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED
    }

}
