package image.module.cdn.service;

import image.module.cdn.client.UrlServiceClient;
import image.module.cdn.dto.ImageDto;
import image.module.cdn.dto.ImageResponseDto;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CdnService {

    private final RedisService redisService;
    private final UrlServiceClient urlServiceClient;

    @Value("${server.port}")
    private static String port;

    public static final String PART_CDN_URL = "http://localhost:" + port + "/cdn/";
    public static final String FILE_PATH = "cdn/src/main/resources/static/images/";


    public ResponseEntity<byte[]> getImage(String cdnUrl) throws IOException {
        String fileLocation = checkFileExist(cdnUrl);

        byte[] imageBytes = getByteImage(fileLocation);

        // 파일의 MIME 타입을 동적으로 추출
        String imageType = getImageType(fileLocation);

        // 응답 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(imageType));

        // 이미지 데이터를 ResponseEntity로 반환
        return new ResponseEntity<>(imageBytes, headers, HttpStatus.OK);

    }

    public ImageResponseDto downloadImage(String cdnUrl) throws IOException {
        String convertedCdnUrl = cdnUrl.replace("/download", "");
        String fileLocation = checkFileExist(convertedCdnUrl);

        ImageResponseDto imageResponseDto = getImageInfo(fileLocation);

        imageResponseDto.getHeaders().setContentDispositionFormData("attachment", getOriginalNameByPath(fileLocation));
        
        return imageResponseDto;
    }

    private ImageResponseDto getImageInfo(String fileLocation) throws IOException {
        ImageResponseDto imageResponseDto = new ImageResponseDto();

        byte[] imageBytes = getByteImage(fileLocation);

        // 파일의 MIME 타입을 동적으로 추출
        String imageType = getImageType(fileLocation);

        // 응답 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(imageResponseDto.getImageType()));

        imageResponseDto.setImageBytes(imageBytes);
        imageResponseDto.setImageType(imageType);
        imageResponseDto.setHeaders(headers);

        return imageResponseDto;
    }

    private byte[] getByteImage(String fileLocation) throws IOException {
        Path imagePath = Paths.get(fileLocation);
        return Files.readAllBytes(imagePath);
    }

    private String getImageType(String fileLocation) throws IOException {
        Path imagePath = Paths.get(fileLocation);
        return Files.probeContentType(imagePath);
    }

    // @Cacheable(cacheNames = "fileLocationCache", key = "args[0]")
    // 같은 클래스에서 한 메서드가 다른 메서드 호출할 때 캐시 적용 안된다고 함
    public String checkFileExist(String cdnUrl) throws IOException {
        String fileLocation = redisService.getValue(cdnUrl);
        if (fileLocation == null) {
            fileLocation = getImageAndSave(cdnUrl);
            redisService.setValue(cdnUrl, fileLocation);
        }
        return fileLocation;
    }

    // 이미지 저장
    private String saveImageInCdn(InputStream imageStream, String fileName) throws IOException {
        // 저장 경로 생성
        Path uploadPath = Paths.get(FILE_PATH);

        // 파일 경로
        Path filePath = uploadPath.resolve(fileName);

        // InputStream을 파일로 복사하여 저장
        Files.copy(imageStream, filePath);

        return filePath.toString();
    }

    private String getImageAndSave(String cdnUrl) throws IOException {
        // fetch server에게 이미지 요청
        ImageDto imageDto = urlServiceClient.fetchImage(URLEncoder.encode(cdnUrl, StandardCharsets.UTF_8));

        // cdn에 저장할 이미지 이름 생성
        String cdnImageName = cdnUrl.replace(PART_CDN_URL, "");
        String saveFileName = imageDto.getFileName() + "_" + cdnImageName;

        return saveImageInCdn(imageDto.getImageStream(), saveFileName);
    }

    private String getOriginalNameByPath(String fileLocation) {
        // FILE_PATH/originalName_cdnImageName(확장자 포함)
        String removeFilePath = fileLocation.replace(FILE_PATH, "");

        int startIndex = removeFilePath.indexOf('_');
        int endIndex = removeFilePath.indexOf('.');

        return removeFilePath.substring(0, startIndex) + removeFilePath.substring(endIndex);
    }
}
