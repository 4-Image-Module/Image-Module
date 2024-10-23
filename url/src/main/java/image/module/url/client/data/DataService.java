package image.module.url.client.data;



import java.util.UUID;

public interface DataService{
    //이미지 이름 조회
    ImageResponse getImageName(UUID id);


    //이미지 cdn -> 이름 조회
    ImageResponse getCDNImageName(String cdnUrl);

    ImageResponse getReCdnUrl(UUID originalFileUUID,Integer size);

}
