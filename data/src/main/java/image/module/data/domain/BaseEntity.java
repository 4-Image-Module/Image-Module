package image.module.data.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @Column(updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime deletedAt;

    @Column(nullable = false)
    private Boolean isDeleted = false;

    ZoneId zoneId = ZoneId.of("Asia/Seoul");
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now(zoneId);  // KST로 저장
        this.updatedAt = LocalDateTime.now(zoneId);  // KST로 저장
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now(zoneId);  // KST로 저장
    }

    public void delete() {
        this.isDeleted = true;
        this.deletedAt = LocalDateTime.now(zoneId);  // KST로 저장
    }
}
