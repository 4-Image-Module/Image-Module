package image.module.data.presentation;

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
}
