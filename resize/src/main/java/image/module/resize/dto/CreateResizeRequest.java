package image.module.resize.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateResizeRequest
{

    private UUID id;
    private String storedFileName;
    private Integer size;
    private String cdnBaseUrl;
    private String type;


    public static CreateResizeRequest create(String storedFileName, Integer size, String cdnBaseUrl) {
        String cdnUrl = cdnBaseUrl + "/" + UUID.randomUUID(); //cdn 이름 확정
        String type = "webp";

        return CreateResizeRequest.builder()
                .storedFileName(storedFileName)
                .size(size)
                .cdnBaseUrl(cdnUrl)
                .type(type)
                .build();
    }
}
