package image.module.data.presentation;

import image.module.data.application.ImageResponse;
import image.module.data.application.ImageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping
public class ImageController {

    private final ImageService imageService;

    @PostMapping("/image/upload")
    public ImageResponse uploadImage(@RequestBody ImageRequest imageRequest){
        return imageService.saveImage(imageRequest);
    }

    //fetch -> 객체 조회
    @GetMapping("/image/getImageName")
    public ImageResponse getImageName(@RequestParam("id") UUID id){

        ImageResponse getImageName = imageService.getImageName(id);

        return getImageName;
    }

    //fetch -> cdn 주소로 객체 조회
    @GetMapping("/image/getCDNImageName")
    public ImageResponse getCDNImageName(@RequestParam("cdnUrl") String cdnUrl){

        ImageResponse getCDNImageName = imageService.getCDNImageName(cdnUrl);

        return getCDNImageName;
    }

    // 원본 이미지 cdnUrl 추가
    @PostMapping("/image/create/cdnUrl")
    public void createCdnUrl(
            @RequestBody OriginalFileInfo originalFileInfo
    ) {
        try {
            imageService.createCdnUrl(originalFileInfo);
            log.info("원본 이미지 cdnUrl 주입 성공");
        }catch (Exception e){
            throw new IllegalArgumentException("원본 이미지 cdnUrl 주입 실패: " + e.getMessage());
        }
    }

    // 리사이즈 WebP 이미지 DB 저장
    @PostMapping("/image/create/resize")
    public void createResizeImage(
            @RequestBody CreateResizeRequest createResizeRequest
    ) {
        imageService.createImage(createResizeRequest); // Image 업데이트 로직

    }

    //원본 uuid 와 size의 cdn url 반환
    @GetMapping("/image/getReCdnUrl")
    public ImageResponse getReCdnUrl(@RequestParam("originalFileUUID") UUID originalFileUUID,
                                     @RequestParam("size") Integer size){
        return imageService.getReCdnUrl(originalFileUUID,size);
    }
}
