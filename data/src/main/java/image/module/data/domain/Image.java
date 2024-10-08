package image.module.data.domain;

import image.module.data.presentation.ImageRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import javax.imageio.ImageIO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Where;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Where(clause = "is_delete = false")
@Entity(name = "image")
public class Image extends BaseEntity {
    @Id
    @GeneratedValue
    @Column(name = "id")
    private UUID id;

    @Column(nullable = false)
    private String originalFileName;

    @Column(nullable = false)
    private String storedFileName;

    @Column(nullable = false)
    private String cdnUrl;

    @Column(nullable = false)
    private String fileType;

    @Column(nullable = false)
    private Integer width;

    @Column(nullable = false)
    private Integer height;

    @Column(name = "original_file_uuid")
    private UUID originalFileUUID;

    @PrePersist
    public void setOriginalFileUUID() {
         this.originalFileUUID = this.id;
    }

    public static Image create(ImageRequest request){
        return Image.builder()
                .originalFileName(request.getOriginalFileName())
                .storedFileName(request.getStoredFileName())
                .cdnUrl(request.getCdnUrl())
                .fileType(request.getFileType())
                .width(request.getWidth())
                .height(request.getHeight())
                .originalFileUUID(request.getOriginalFileUUID())
                .build();
    }
}
