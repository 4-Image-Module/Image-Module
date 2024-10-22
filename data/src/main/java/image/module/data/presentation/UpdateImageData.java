package image.module.data.presentation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateImageData {
  private UUID id;
  private String storedFileName;
  private String cdnBaseUrl;
}
