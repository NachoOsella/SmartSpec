package com.smartspec.model.entity;

import java.util.ArrayList;
import java.util.List;

import com.smartspec.model.enums.PriorityEnum;
import com.smartspec.model.enums.StatusEnum;

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
@ToString(exclude = { "project", "stories" })
@Entity
@Table(name = "epics")
public class EpicEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PriorityEnum priority;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusEnum status;

    @Column(name = "order_index")
    private Integer orderIndex;

    @Builder.Default
    @OneToMany(mappedBy = "epic", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserStoryEntity> stories = new ArrayList<>();

    public void addStory(UserStoryEntity story) {
        stories.add(story);
        story.setEpic(this);
    }

    public void removeStory(UserStoryEntity story) {
        stories.remove(story);
        story.setEpic(null);
    }
}
