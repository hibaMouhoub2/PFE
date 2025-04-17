package com.example.projetpfe.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name="audit_logs")
public class Audit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id ;

    @Enumerated(EnumType.STRING)
    private AuditType type;

    private String entityType;
    private Long entityId;

    @Column(length = 1000)
    private String details;

    @ManyToOne
    @JoinColumn(name= "user_id")
    private User user ;


    private LocalDateTime timestamp;
    // Méthode utilitaire pour créer facilement une entrée d'audit
    public static Audit create(AuditType type, String entityType, Long entityId, String details, User user) {
        Audit audit = new Audit();
        audit.setType(type);
        audit.setEntityType(entityType);
        audit.setEntityId(entityId);
        audit.setDetails(details);
        audit.setUser(user);
        audit.setTimestamp(LocalDateTime.now());
        return audit;
    }

}
