package image.module.url.client.data;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;



@FeignClient(name = "data")

public interface DataClient extends DataService {


    //이미지 이름 조회
    @GetMapping("/image/getImageName")
    ImageResponse getImageName(@RequestParam("id") UUID id);


    //이미지 cdn -> 이름 조회
    @GetMapping("/image/getCDNImageName")
    ImageResponse getCDNImageName(@RequestParam("cdnUrl") String cdnUrl);


    //resizing 된 cdn url 반환
    @GetMapping("/image/getReCdnUrl")
    ImageResponse getReCdnUrl(@RequestParam("originalFileUUID") UUID originalFileUUID,
                              @RequestParam("size") Integer size);
    }

