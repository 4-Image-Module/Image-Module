package image.module.url.controller;


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


    private final UrlService urlService;


    public UrlController( UrlService urlService) {
        this.urlService = urlService;
    }


    //uuid 조회시 cdn Url 반환
    @GetMapping("/cdnUrl")
    public ResponseEntity<String> getImage(@RequestParam("id") UUID id,
                                           @RequestParam(value = "size", required = false ) Integer size){



        //원본 : size null 일 때
        if(size==null){
            return urlService.getCdnUrl(id);
        }

        //리사이징 된 이미지 조회

        return urlService.getReCdnUrl(id, size);
    }



    // 이미지 바이트 배열로 반환하는 FeignClient용 api
    @GetMapping("/image/byte")
    public ResponseEntity<byte[]> fetchImageByte(@RequestParam("cdnUrl") String cdnUrl) {

        return urlService.fetchImageByte(cdnUrl);
    }
}


