package image.module.convert.service;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.webp.WebpWriter;
import image.module.convert.DataClient;
import image.module.convert.dto.SendKafkaMessage;
import image.module.convert.dto.UpdateImageData;
import image.module.convert.dto.OriginalImageResponse;
import io.minio.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.imgscalr.Scalr;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Service
public class ImageService {
  @Value("${minio.buckets.downloadBucket}")
  private String originalBucket;

  @Value("${minio.buckets.uploadBucket}")
  private String uploadBucket;

  @Value("${cdn-server.url}")
  private String cdnBaseUrl;

  private final MinioClient minioClient;
  private final DataClient dataClient;

  private final KafkaTemplate<String, SendKafkaMessage> kafkaTemplate;

  public ImageService(MinioClient minioClient, DataClient dataClient, KafkaTemplate<String, SendKafkaMessage> kafkaTemplate) {
    this.minioClient = minioClient;
    this.dataClient = dataClient;
    this.kafkaTemplate = kafkaTemplate;
  }

  // 전체 이미지 처리 로직을 관리하는 메서드
  @KafkaListener(topics = "image-upload-topic", groupId = "image-upload-group")
  public void removeMetadataAndCovertWebP(OriginalImageResponse originalImage) {
    String StoredFileName = originalImage.getStoredFileName();
    Integer size = originalImage.getRequestSize();

    // 확장자 추출
    String extension = extractExtensionFromMinio(StoredFileName);

    // 이미지 다운로드
    InputStream originalFile = downloadImage(StoredFileName); // 다운로드

    // 원본 이미지 복사
    File copyOriginalFile = copyOriginalImage(originalFile, extension);

    //  4. MINIO 원본 이미지 삭제
    removeOriginalImage(StoredFileName);

    // 5. EXIF 메타 데이터 삭제 및 이미지 회전 처리
    File checkedRotate = removeMetadataAndFixOrientation(copyOriginalFile, extension);
    // MINIO로 업로드
    uploadImageToMinio(checkedRotate, StoredFileName, extension);

    // 6. 복사 이미지 WebP로 변환
    File webpFile = convertToWebp(StoredFileName, copyOriginalFile);

    // 7. WebP 이미지 업로드
    uploadWebPImage(webpFile);

    // 8. 원본 이미지 cdnUrl 추가
    UpdateImageData updateImageDataInfo = UpdateImageData.create(StoredFileName, cdnBaseUrl);
    dataClient.updateImageData(updateImageDataInfo);

    kafkaTemplate.send("image-resize-topic", SendKafkaMessage.createMessage(webpFile.getName(), size));

    // 9. 임시 파일 삭제
    cleanupTemporaryFiles(copyOriginalFile, checkedRotate, webpFile);
  }


  // 확장자 추출 메서드
  private String extractExtensionFromMinio(String fileName) {
    try {
      StatObjectResponse statObject = minioClient.statObject(
              StatObjectArgs.builder()
                      .bucket(originalBucket)
                      .object(fileName)
                      .build()
      );

      // Content-Type 출력
      String contentType = statObject.contentType(); // ex) image/jpeg
      // Content-Type에서 확장자 가져오기 / ex) jpeg
      return extractExtensionFromContentType(contentType);

    } catch (Exception e) {
      throw new RuntimeException("확장자 추출 실패: " + e.getMessage(), e);
    }
  }

  // 확장자 추출
  private String extractExtensionFromContentType(String contentType) {
    if ("image/jpeg".equals(contentType) || "image/jpg".equals(contentType)) {
      return "jpg";
    } else if ("image/png".equals(contentType)) {
      return "png";
    } else {
      throw new IllegalArgumentException("지원되지 않는 파일 형식입니다: " + contentType);
    }
  }

  // 이미지 다운로드
  public InputStream downloadImage(String fileName) {
    try {
      return minioClient.getObject(
              GetObjectArgs.builder()
                      .bucket(originalBucket)
                      .object(fileName)
                      .build()
      );
    } catch (Exception e) {
      throw new IllegalArgumentException("이미지 다운로드 실패: " + e.getMessage());
    }
  }

  // 원본 이미지 복사
  private File copyOriginalImage(InputStream originalFile, String extension) {
    File copyOriginalFile = null;
    try {
      copyOriginalFile = File.createTempFile("copy-", "." + extension); // 임시 파일 생성
      FileUtils.copyInputStreamToFile(originalFile, copyOriginalFile); // 생성된 임시 파일에 원본 파일 복사
    } catch (IOException e) {
      throw new IllegalArgumentException("이미지 복사 실패: " + e.getMessage());
    }
    return copyOriginalFile;
  }

  // 3. MINIO의 원본 이미지 삭제
  private void removeOriginalImage(String fileName) {
    try {
      minioClient.removeObject(
              RemoveObjectArgs.builder()
                      .bucket(originalBucket)
                      .object(fileName)
                      .build()
      );
    } catch (Exception e) {
      throw new RuntimeException("이미지 삭제 실패: " + e.getMessage());
    }
  }

  // 5. EXIF 메타 데이터 삭제 및 이미지 회전 처리
  private File removeMetadataAndFixOrientation(File copyOriginalFile, String extension) {
    try {

      BufferedImage bufferedImage = ImageIO.read(copyOriginalFile);

      int orientation = 1;
      Metadata metadata;
      Directory directory;

      try {
        metadata = ImageMetadataReader.readMetadata(copyOriginalFile);
        directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
        if (directory != null) {
          orientation = directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
        }
      } catch (Exception e) {
        orientation = 1; // 메타데이터 읽기 실패 시 기본 방향(1) 설정
      }

      switch (orientation) {
        case 3:
          bufferedImage = Scalr.rotate(bufferedImage, Scalr.Rotation.CW_180);
          break;
        case 6:
          bufferedImage = Scalr.rotate(bufferedImage, Scalr.Rotation.CW_90);
          break;
        case 8:
          bufferedImage = Scalr.rotate(bufferedImage, Scalr.Rotation.CW_270);
          break;
        default:
          break; // orientation 1일 때는 아무 작업도 하지 않음
      }


      // 임시 파일 생성
      File imageRotateFile = File.createTempFile("rotate-", "." + extension);
      // 새로운 파일로 저장
      ImageIO.write(bufferedImage, extension, imageRotateFile);

      return imageRotateFile;
    } catch (Exception e) {
      throw new IllegalArgumentException("메타 데이터 삭제 및 이미지 회전 오류 발생: " + e.getMessage(), e);
    }
  }

  // MINIO로 업로드
  private void uploadImageToMinio(File file, String fileName, String extension) {
    try {
      // MINIO에 파일 업로드
      minioClient.putObject(
              PutObjectArgs.builder()
                      .bucket(originalBucket)
                      .object(fileName) // 동일한 이름으로 업로드
                      .stream(new FileInputStream(file), file.length(), -1)
                      .contentType("image/" + extension) // 업로드할 이미지의 content type 지정
                      .build()
      );
    } catch (Exception e) {
      throw new RuntimeException("이미지 업로드 실패: " + e.getMessage());
    }
  }


  // WebP 파일 업로드
  private void uploadWebPImage(File webpFile) {
    try (InputStream webpInputStream = new FileInputStream(webpFile)) {
      minioClient.putObject(PutObjectArgs.builder()
              .bucket(uploadBucket)
              .object(webpFile.getName())
              .stream(webpInputStream, webpFile.length(), -1)
              .contentType("image/webp")
              .build());
    } catch (Exception e) {
      throw new IllegalArgumentException("WebP 파일 업로드 실패: " + e.getMessage());
    }
  }

  // 복사 이미지 WebP 파일로 변환
  public File convertToWebp(String fileName, File copyOriginalFile) {
    try {
      String uploadFileName = FilenameUtils.getBaseName(fileName) + ".webp"; // MINIO에 업로드할 최종 파일 이름
      File outputFile = new File(copyOriginalFile.getParent(), uploadFileName);

      return ImmutableImage.loader()
              .fromFile(copyOriginalFile)
              .output(WebpWriter.DEFAULT, outputFile); // 손실 압축
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage());
    }
  }

  // 임시 파일 삭제
  private void cleanupTemporaryFiles(File... files) {
    for (File file : files) {
      if (file.exists()) {
        boolean deleted = file.delete();
        if (!deleted) {
          throw new IllegalArgumentException("임시 파일 삭제 실패: " + file.getAbsolutePath());
        }
      }
    }
  }
}