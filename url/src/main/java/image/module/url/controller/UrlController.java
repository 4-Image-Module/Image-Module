package image.module.url.controller;

import image.module.url.client.data.DataService;
import image.module.url.client.data.ImageResponse;
import image.module.url.service.UrlService;
import java.util.UUID;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@RestController
@RequestMapping("/fetch")
public class UrlController {

    private final DataService dataService;
    private final UrlService urlService;


    public UrlController(DataService dataService, UrlService urlService) {
        this.dataService = dataService;
        this.urlService = urlService;
    }


   //uuid 조회시 cdn Url 반환
    @GetMapping("/cdnUrl")
    public ResponseEntity<String> getImage(@RequestParam("id") UUID id) {

        // 1. UUID로 cdnUrl 조회
        ImageResponse imageResponse = dataService.getImageName(id);

        // cdnUrl이 null인지 확인
        if (imageResponse == null || imageResponse.getCdnUrl() == null) {
            String message = "이미지를 찾을 수 없거나 CDN URL이 없습니다.";
            log.warn(message);
            return ResponseEntity.ok(message); // 200 OK와 함께 메시지 반환
        }

        String cdnUrl = imageResponse.getCdnUrl();
        log.info("CDN URL: {}", cdnUrl);

        return ResponseEntity.ok(cdnUrl); // 정상적으로 CDN URL 반환
    }



    // 이미지 바이트 배열로 반환하는 FeignClient용 api
    @GetMapping("/image/byte")
    public ResponseEntity<byte[]> fetchImageByte(@RequestParam("cdnUrl") String cdnUrl) {

        return urlService.fetchImageByte(cdnUrl);
    }
}


