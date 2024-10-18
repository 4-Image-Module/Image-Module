package image.module.upload.application;

import image.module.upload.domain.ImageExtension;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageRequest {

    private UUID id;
    private String originalFileName;
    private String storedFileName;
    private String cdnUrl;
    private String fileType;
    private Integer size;
    private Integer cashingTime;

    public static ImageRequest create(String originalName,
                                      String extension,
                                      String cdnBaseUrl,
                                      int size,
                                      int cashingTime) {
        // 1. UUID 생성
        String uuid = UUID.randomUUID().toString();

        // 2. 현재 시간 포맷팅
        String formattedTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        // 3. 저장할 파일 이름 생성 (이미지 이름 치환하기)
        String storedFileName = uuid + "_" + formattedTime;

        // 5. cdn url 생성
        String cdnUrl = cdnBaseUrl + "/" + UUID.randomUUID() + "." + extension;

        return ImageRequest.builder()
                .originalFileName(originalName)
                .storedFileName(storedFileName)
                .cdnUrl(cdnUrl)
                .fileType(extension)
                .size(size)
                .cashingTime(cashingTime)
                .build();
    }
}
