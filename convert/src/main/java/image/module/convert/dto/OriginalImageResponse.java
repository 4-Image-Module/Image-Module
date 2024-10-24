package image.module.convert.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
public class OriginalImageResponse {
    private String storedFileName;
    private Integer requestSize;
}