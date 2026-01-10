package com.planai.model.entity;

import java.util.ArrayList;
import java.util.List;

import com.planai.model.enums.PriorityEnum;
import com.planai.model.enums.StatusEnum;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"epic", "tasks"})
@Entity
@Table(name = "user_stories")
public class UserStoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "epic_id", nullable = false)
    private EpicEntity epic;

    @Column(nullable = false)
    private String title;

    @Column(name = "as_a", length = 500)
    private String asA;

    @Column(name = "i_want", length = 500)
    private String iWant;

    @Column(name = "so_that", length = 500)
    private String soThat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PriorityEnum priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusEnum status;

    @Column(name = "order_index")
    private Integer orderIndex;

    @Builder.Default
    @OneToMany(mappedBy = "userStory", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TaskEntity> tasks = new ArrayList<>();

    public void addTask(TaskEntity task) {
        tasks.add(task);
        task.setUserStory(this);
    }

    public void removeTask(TaskEntity task) {
        tasks.remove(task);
        task.setUserStory(null);
    }
}
