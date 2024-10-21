package image.module.data.domain;

import image.module.data.presentation.CreateResizeRequest;
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

import lombok.*;
import org.hibernate.annotations.Where;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Where(clause = "is_deleted = false")
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

    private String cdnUrl;

    @Column(nullable = false)
    private String fileType;

    @Column(nullable = false)
    private Integer size;

    @Column(nullable = false)
    private Integer cachingTime;

    @Column(name = "original_file_uuid")
    private UUID originalFileUUID;

    public static Image create(ImageRequest request){
        return Image.builder()
                .originalFileName(request.getOriginalFileName())
                .storedFileName(request.getStoredFileName())
                .fileType(request.getFileType())
                .size(request.getSize())
                .cachingTime(request.getCachingTime())
                .build();
    }

    public void updateImageData(Integer size, String cdnUrl){
        this.size = size;
        this.cdnUrl = cdnUrl;
    }

    public void assignOriginalFileUUID(){
        this.originalFileUUID = this.id;
    }

    public static Image createResize(Image image, CreateResizeRequest createResizeRequest){
        return Image.builder()
                .originalFileName(image.getOriginalFileName())
                .storedFileName(createResizeRequest.getStoredFileName() + "_" + createResizeRequest.getSize())
                .cdnUrl(createResizeRequest.getCdnBaseUrl())
                .fileType(createResizeRequest.getType())
                .size(createResizeRequest.getSize())
                .cachingTime(image.getCachingTime())
                .originalFileUUID(image.getOriginalFileUUID())
                .build();
    }

}
