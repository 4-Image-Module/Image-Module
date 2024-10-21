package image.module.resize.service;

import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.webp.WebpWriter;
import image.module.resize.dto.ReceiveKafkaMessage;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Service
public class ResizeService {

  @Value("${minio.buckets.uploadBucket}")
  private String uploadBucket;

  @Value("${cdn-server.url}")
  private String cdnBaseUrl;

  private final MinioClient minioClient;

  public ResizeService(MinioClient minioClient) {
    this.minioClient = minioClient;
  }

  @KafkaListener(topics = "image-resize-topic", groupId = "image-resize-group")
  public void ResizeImage(ReceiveKafkaMessage receiveKafkaMessage) {
    String webPFileName = receiveKafkaMessage.getWebPFileName();
    Integer size = receiveKafkaMessage.getSize();
    log.info("@@@@@ webPFileName: " + webPFileName); // .webp


    // 0. .webp 지우기 -> UUID_날짜
    String uploadName = extractUuidAndDate(webPFileName);
    log.info("@@@@@ uploadName: " + uploadName);

    // 1. MINIO에서 기존에 있던 webpFile 다운로드
    InputStream originalFile = downloadImage(webPFileName);

    // 2. 이미지 복사
    File copyOriginalFile = copyOriginalImage(originalFile);
    log.info("@@@@@ copyOriginalFile: " + copyOriginalFile.getName());

    // 3. MINIO에서 기존에 있던 webpFile 삭제
    removeOriginalWebPImage(webPFileName);

    // 4. 가로 세로 비교 / 둘 중 큰 변을 기준으로 리사이즈
    File resizeFile = resizeImage(uploadName, copyOriginalFile, size);
    log.info("@@@@@#### resizeFileName: " + resizeFile.getName());

    // 5. 리사이즈 된 이미지 업로드
    uploadWebPImage(resizeFile);

//    // 6. 리사이징 된 WebP 이미지 DB 생성
//    CreateResizeRequest createResizeImageInfo = CreateResizeRequest.create(uploadName, size, cdnBaseUrl);
//    dataClient.createResizeImage(createResizeImageInfo);

    // 임시 파일 삭제

  }

  public String extractUuidAndDate(String fileName) {
    // 파일 이름에서 마지막 점(.)의 위치 찾기
    int dotIndex = fileName.lastIndexOf(".");

    if (dotIndex != -1) {
      // 마지막 점(.) 앞부분까지만 잘라내기
      return fileName.substring(0, dotIndex);
    } else {
      throw new IllegalArgumentException("파일 이름에 확장자가 포함되어 있지 않습니다.");
    }
  }

  // 1. MINIO에서 기존에 있던 webpFile 다운로드
  public InputStream downloadImage(String webPFileName) {
    try {
      return minioClient.getObject(
              GetObjectArgs.builder()
                      .bucket(uploadBucket)
                      .object(webPFileName)
                      .build()
      );
    } catch (Exception e) {
      throw new IllegalArgumentException("이미지 다운로드 실패: " + e.getMessage());
    }
  }

  // 2. 이미지 복사
  private File copyOriginalImage(InputStream originalFile) {
    File copyOriginalFile = null;
    try {
      copyOriginalFile = File.createTempFile("copy-", ".webp"); // 임시 파일 생성
      FileUtils.copyInputStreamToFile(originalFile, copyOriginalFile); // 생성된 임시 파일에 원본 파일 복사
    } catch (IOException e) {
      throw new IllegalArgumentException("원본 이미지 복사 실패: " + e.getMessage());
    }
    return copyOriginalFile;
  }

  // 3. MINIO의 원본 이미지 삭제
  private void removeOriginalWebPImage(String webPFileName) {
    try {
      minioClient.removeObject(
              RemoveObjectArgs.builder()
                      .bucket(uploadBucket)
                      .object(webPFileName)
                      .build()
      );
    } catch (Exception e) {
      throw new RuntimeException("이미지 삭제 실패: " + e.getMessage());
    }
  }


  public File resizeImage(String uploadName, File copyOriginalFile, Integer resizingSize) {
    // 리사이즈된 이미지를 저장할 파일
    File resizedFile = new File(copyOriginalFile.getParent(), uploadName + "_" + resizingSize); // MINIO에 업로드 될 최종 이름

    try {
      // Scrimage로 원본 이미지 로드
      ImmutableImage image = ImmutableImage.loader().fromFile(copyOriginalFile);

      // 원본 이미지의 너비와 높이 정보 출력
      int width = image.width;
      int height = image.height;
      log.info("##### width: " + width);
      log.info("##### height: " + height);

      // 가로 세로 비율에 맞춰 리사이즈
      ImmutableImage resizedImage;
      if (width >= height) {
        // 가로 사이즈에 맞춰 리사이즈
        resizedImage = image.scaleToWidth(resizingSize); // 가로 기준 리사이즈
      } else {
        // 세로 사이즈에 맞춰 리사이즈
        resizedImage = image.scaleToHeight(resizingSize); // 세로 기준 리사이즈
      }

      log.info("####### resizedImage.width: " + resizedImage.width);
      log.info("####### resizedImage.height: " + resizedImage.height);

      return resizedImage.output(WebpWriter.DEFAULT, resizedFile);

    } catch (IOException e) {
      log.error("이미지 리사이징 실패: " + e.getMessage(), e);
      throw new IllegalArgumentException("이미지 리사이징 실패: " + e.getMessage());
    }
  }

  // WebP 파일 업로드
  private void uploadWebPImage(File resizingFile) {
    try (InputStream webpInputStream = new FileInputStream(resizingFile)) {
      minioClient.putObject(PutObjectArgs.builder()
              .bucket(uploadBucket)
              .object(resizingFile.getName())
              .stream(webpInputStream, resizingFile.length(), -1)
              .contentType("image/webp")
              .build());
    } catch (Exception e) {
      throw new IllegalArgumentException("WebP 파일 업로드 실패: " + e.getMessage());
    }
  }


}
