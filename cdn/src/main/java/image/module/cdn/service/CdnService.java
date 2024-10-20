package image.module.cdn.service;

import image.module.cdn.client.UrlServiceClient;
import image.module.cdn.dto.ImageDto;
import image.module.cdn.dto.ImageResponseDto;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CdnService {

    private final RedisService redisService;
    private final UrlServiceClient urlServiceClient;

    // @Value는 static 변수에 주입되지 않는다고 함
    @Value("${server.port}")
    private String port;

    @Value("${cdn.image.path}")
    public String filePath;

    @Value("${cdn.image.url}")
    public String fileUrl;

    public String getPartCdnUrl() {
        return "http://" + fileUrl + ":" + port + "/cdn/";
    }

    public ImageResponseDto getImage(String cdnUrl) throws IOException {
        String fileLocation = checkFileExist(cdnUrl);
        return getImageInfo(fileLocation);
    }

    public ImageResponseDto downloadImage(String cdnUrl) throws IOException {
        String convertedCdnUrl = cdnUrl.replace("/download", "");
        String fileLocation = checkFileExist(convertedCdnUrl);

        ImageResponseDto imageResponseDto = getImageInfo(fileLocation);

        String imageOriginalName = getOriginalNameByPath(fileLocation) + "." + getImageType(fileLocation);

        imageResponseDto.getHeaders().setContentDispositionFormData("attachment", imageOriginalName);

        return imageResponseDto;
    }

    private ImageResponseDto getImageInfo(String fileLocation) throws IOException {
        ImageResponseDto imageResponseDto = new ImageResponseDto();

        byte[] imageBytes = getByteImage(fileLocation);

        // 파일의 MIME 타입을 동적으로 추출
        String imageType = getImageType(fileLocation);

        // 응답 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(imageType));

        imageResponseDto.setImageBytes(imageBytes);
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
        }
        return fileLocation;
    }

    // 이미지 저장
    private String saveImageInCdn(byte[] imageBytes, String fileName) throws IOException {
        log.info("이미지 저장 시도");
        // 저장 경로 생성
        Path uploadPath = Paths.get(filePath);

        // 파일 경로
        Path filePath = uploadPath.resolve(fileName);

        // 이미지 저장
        Files.write(filePath, imageBytes);

        log.info("이미지 저장 완료");

        return filePath.toString();
    }

    private String getImageAndSave(String cdnUrl) throws IOException {
        // fetch server에게 이미지 요청
        ImageDto imageDto = urlServiceClient.fetchImage(URLEncoder.encode(cdnUrl, StandardCharsets.UTF_8));
        log.info("이미지 이름 " + imageDto.getFileName());

        // cdn에 저장할 이미지 이름 생성
        String cdnImageName = cdnUrl.replace(getPartCdnUrl(), "");
        String saveFileName = imageDto.getFileName() + "_" + cdnImageName;

        // String fileLocation = saveImageInCdn(imageDto.getImageStream(), saveFileName);

        byte[] imageByte = urlServiceClient.fetchImageByte(URLEncoder.encode(cdnUrl, StandardCharsets.UTF_8)).getBody();

        String fileLocation = saveImageInCdn(imageByte, saveFileName);

        // TODO: FeignClient에서 추후 caching time 받아서 처리하도록 수정
        redisService.setValue(cdnUrl, fileLocation, imageDto.getCachingTime());
        redisService.setBackupValue(cdnUrl, fileLocation);

        return fileLocation;
    }

    // 저장된 이미지 이름에서 원본 이미지 뽑는 메서드
    private String getOriginalNameByPath(String fileLocation) {
        // FILE_PATH/originalName_cdnImageName - FILE_PATH/
        String removeFilePath = fileLocation.replace(filePath, "");

        int removedIndex = removeFilePath.indexOf('_');

        if (removedIndex == -1) {
            throw new IllegalArgumentException("잘못된 이미지 이름 형식 입니다: " + removeFilePath);
        }

        // originalName_cdnImageName - _cdnImageName
        return removeFilePath.substring(0, removedIndex);
    }
}
