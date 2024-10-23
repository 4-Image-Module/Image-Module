package image.module.convert.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateImageData {

    private UUID id;
    private String storedFileName;
    private String cdnBaseUrl;

    public static UpdateImageData create(String storedFileName, String cdnBaseUrl) {
        String cdnUrl = cdnBaseUrl + "/" + UUID.randomUUID(); //cdn 이름 확정

        return UpdateImageData.builder()
                .storedFileName(storedFileName)
                .cdnBaseUrl(cdnUrl)
                .build();
    }
}
