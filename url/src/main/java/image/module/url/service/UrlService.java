package image.module.url.service;

import image.module.url.client.data.DataService;
import image.module.url.client.data.ImageResponse;
import image.module.url.dto.ImageDto;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import javax.imageio.ImageIO;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class UrlService {

    private final DataService dataService;
    private final MinioClient minioClient;

    @Value("${minio.buckets.uploadBucket}")
    private String uploadBucket;

    @Value("${minio.buckets.downloadBucket}")
    private String downloadBucket;

    public ImageDto fetchImage(String cdnUrl) {
        // CDN URL을 통해 데이터베이스에서 파일 이름 조회
        log.info("받은 CDN URL: {}", cdnUrl);
        ImageResponse imageResponse = dataService.getCDNImageName(URLEncoder.encode(cdnUrl, StandardCharsets.UTF_8));

        // imageResponse가 null인지 확인
        if (imageResponse == null) {
            throw new RuntimeException("제공된 CDN URL에 대한 이미지를 찾을 수 없습니다.");
        }

        String fileName = imageResponse.getStoredFileName();
        String fileType = imageResponse.getFileType();
        log.info("저장된 파일명: {}", fileName);
        log.info("파일 타입: {}", fileType);

        // ImageDto 생성 및 반환
        return new ImageDto(fileName);
    }


    public ResponseEntity<byte[]> fetchImageByte(String cdnUrl) {
        try {
            log.info("받은 CDN URL: {}", cdnUrl);

            //data server에서 image 정보 조회 메서드
            ImageResponse imageResponse = getImageResponse(cdnUrl);

            // 조회한 이미지에서 storedFileName , originalFileName , fileType 조회
            String fileName = imageResponse.getOriginalFileName();
            String storegeFileName = imageResponse.getStoredFileName();
            String fileType = imageResponse.getFileType();
            Integer cache = imageResponse.getCachingTime();

            log.info("저장된 파일명: {}", fileName);
            log.info("파일 타입: {}", fileType);

            // file type이 webp이면 minio bucket uploadBucket으로 조회, 그 외는 downloadBucket에서 조회
            String bucketToUse = fileType.equalsIgnoreCase("webp") ? uploadBucket : downloadBucket;

            //minio에서 이미지 파일 가져오는 것
            InputStream inputStream = getImageInputStream(bucketToUse, storegeFileName);

            // 이미지 변환 및 바이트 배열 변환 로직
            byte[] imageBytes = convertImageToBytes(inputStream, fileType);

            if (imageBytes == null || imageBytes.length == 0) {
                throw new RuntimeException("이미지 바이트가 null이거나 비어 있습니다.");
            }

            log.info("이미지 바이트 길이: {}", imageBytes.length);
            // 헤더 설정
            // 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.add("fileName", fileName);
            headers.add("File-Type", fileType);
            headers.add("cache-time", cache.toString());
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);


            // ResponseEntity 반환
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(imageBytes);

        } catch (IOException e) {
            log.error("입출력 오류 발생: {}", e.getMessage());
            throw new RuntimeException("이미지 처리 중 오류 발생: " + e.getMessage());
        } catch (Exception e) {
            log.error("예상치 못한 오류 발생: {}", e.getMessage());
            throw new RuntimeException("이미지 가져오기 중 오류 발생: " + e.getMessage());
        }
    }

    //이미지 파일 이름 조회 부분 분리
    private ImageResponse getImageResponse(String cdnUrl) throws UnsupportedEncodingException {
        String encodedUrl = URLEncoder.encode(cdnUrl, StandardCharsets.UTF_8);
        ImageResponse imageResponse = dataService.getCDNImageName(encodedUrl);

        if (imageResponse == null) {
            throw new RuntimeException("제공된 CDN URL에 대한 이미지를 찾을 수 없습니다.");
        }

        return imageResponse;
    }

    //MiniO에서 이미지 파일 가져오는 부분 분리

    private InputStream getImageInputStream(String bucket, String storegeFileName) throws Exception {
        InputStream inputStream = minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucket)
                .object(storegeFileName)
                .build());

        if (inputStream == null) {
            throw new RuntimeException(storegeFileName+ " 파일에 대한 InputStream이 null입니다.");
        }

        return inputStream;
    }

    // 이미지 변환 및 바이트 배열 변환 로직 분리

    private byte[] convertImageToBytes(InputStream inputStream, String fileType) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        if ("webp".equalsIgnoreCase(fileType)) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                throw new IOException("지원하지 않는 이미지 형식이거나 손상된 이미지입니다.");
            }
            ImageIO.write(image, "jpg", outputStream); // JPG로 변환
        } else {
            inputStream.transferTo(outputStream);
        }

        return outputStream.toByteArray();
    }







}
