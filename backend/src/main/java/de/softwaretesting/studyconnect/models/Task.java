package de.softwaretesting.studyconnect.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * Task entity following the specification in docs/spec.md. - supports multiple assignees - supports
 * tags as an ElementCollection - includes helper methods for entity management
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

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by_id")
  private User createdBy;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "task_assignees",
      joinColumns = @JoinColumn(name = "task_id"),
      inverseJoinColumns = @JoinColumn(name = "user_id"))
  private Set<User> assignees = new HashSet<>();

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "group_id")
  @JsonBackReference
  private Group group;

  @Column(name = "last_status_change_at", nullable = false)
  private LocalDateTime lastStatusChangeAt;

  // Helper methods and variables

  @Transient private Status originalStatus;

  @PostLoad
  void onLoad() {
    this.originalStatus = this.status;
  }

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
    this.lastStatusChangeAt = LocalDateTime.now();
  }

  public void setInProgress() {
    this.status = Status.IN_PROGRESS;
    this.lastStatusChangeAt = LocalDateTime.now();
  }

  public boolean isOverdue() {
    if (dueDate == null) return false;
    return status != Status.COMPLETED && dueDate.isBefore(LocalDateTime.now());
  }

  public void setGroup(Group group) {
    this.group = group;
  }

  @PrePersist
  void onCreate() {
    if (this.createdAt == null) this.createdAt = LocalDateTime.now();
    this.updatedAt = this.createdAt;
    this.lastStatusChangeAt = this.createdAt;
  }

  @PreUpdate
  void onUpdate() {
    this.updatedAt = LocalDateTime.now();
    // Update lastStatusChangeAt if status has changed
    if (this.originalStatus != this.status) {
      this.lastStatusChangeAt = this.updatedAt;
      this.originalStatus = this.status; // avoid repeated updates on multiple flushes
    }
  }

  // Inner enums for Priority and Status to keep them scoped with Task
  public enum Priority {
    LOW,
    MEDIUM,
    HIGH
  }

  public enum Status {
    OPEN,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED
  }
}
