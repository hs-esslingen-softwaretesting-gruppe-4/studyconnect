package de.softwaretesting.studyconnect.models;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@ToString(exclude = {"tasks", "members", "admins", "createdBy"})
@Table(name = "groups")
public class Group {
  private static final SecureRandom INVITE_CODE_RANDOM = new SecureRandom();
  private static final int INVITE_CODE_BYTES = 16;

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @Column(name = "id", nullable = false)
  private Long id;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Column(name = "description", length = 500)
  private String description;

  @Column(name = "max_members", nullable = false)
  private Integer maxMembers = 20;

  @Column(name = "is_public", nullable = false)
  private boolean isPublic = false;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by", nullable = false)
  private User createdBy;

  @OneToMany(
      mappedBy = "group",
      cascade = CascadeType.ALL,
      orphanRemoval = true,
      fetch = FetchType.LAZY)
  @JsonManagedReference
  private List<Task> tasks = new ArrayList<>();

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @ManyToMany
  @JoinTable(
      name = "group_members",
      joinColumns = @JoinColumn(name = "group_id"),
      inverseJoinColumns = @JoinColumn(name = "user_id"))
  private Set<User> members = new HashSet<>();

  @ManyToMany
  @JoinTable(
      name = "group_admins",
      joinColumns = @JoinColumn(name = "group_id"),
      inverseJoinColumns = @JoinColumn(name = "user_id"))
  private Set<User> admins = new HashSet<>();

  @Column(name = "invite_code", nullable = false, unique = true, length = 50)
  private String inviteCode;

  @Column(name = "member_count", nullable = false)
  private int memberCount;

  @PrePersist
  protected void onCreate() {
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
    if (this.inviteCode == null || this.inviteCode.isBlank()) {
      this.inviteCode = generateInviteCode();
    }
    this.memberCount = this.members == null ? 0 : this.members.size();
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = LocalDateTime.now();
  }

  private static String generateInviteCode() {
    byte[] bytes = new byte[INVITE_CODE_BYTES];
    INVITE_CODE_RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  public void regenerateInviteCode() {
    this.inviteCode = generateInviteCode();
  }

  public boolean addMember(User user) {
    if (this.members.size() >= this.maxMembers) {
      return false;
    }
    boolean added = this.members.add(user);
    if (added) {
      this.memberCount = this.members.size();
    }
    return added;
  }

  public boolean removeMember(User user) {
    boolean removed = this.members.remove(user);
    if (removed) {
      this.memberCount = this.members.size();
    }
    return removed;
  }

  public void addTask(Task task) {
    if (task == null) return;
    if (!this.tasks.contains(task)) {
      this.tasks.add(task);
    }
    // avoid infinite loop if already set
    if (task.getGroup() == null || !this.equals(task.getGroup())) {
      task.setGroup(this);
    }
  }

  public void removeTask(Task task) {
    if (task == null) return;
    if (this.tasks.remove(task)) {
      // if orphanRemoval or cascade remove is desired, nulling group will allow removal
      task.setGroup(null);
    }
  }

  public boolean addAdmin(User user) {
    return this.admins.add(user);
  }

  public void setAdmin(User user) {
    this.admins.clear();
    if (user != null) {
      this.admins.add(user);
    }
  }

  public boolean removeAdmin(User user) {
    return this.admins.remove(user);
  }

  public boolean isAdmin(User user) {
    return this.admins.contains(user);
  }
}
