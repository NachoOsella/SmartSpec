package com.smartspec.model.entity;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
@ToString(exclude = { "epics", "conversations" })
@Entity
@Table(name = "projects")
public class ProjectEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @Builder.Default
    @OneToMany(mappedBy = "project", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EpicEntity> epics = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "project", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ConversationEntity> conversations = new ArrayList<>();

    public void addEpic(EpicEntity epic) {
        epics.add(epic);
        epic.setProject(this);
    }

    public void removeEpic(EpicEntity epic) {
        epics.remove(epic);
        epic.setProject(null);
    }

    public void addConversation(ConversationEntity conversation) {
        conversations.add(conversation);
        conversation.setProject(this);
    }

    public void removeConversation(ConversationEntity conversation) {
        conversations.remove(conversation);
        conversation.setProject(null);
    }
}
