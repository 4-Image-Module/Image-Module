package image.module.data.application;

import image.module.data.domain.Image;
import image.module.data.domain.repository.ImageRepository;
import image.module.data.presentation.CreateResizeRequest;
import image.module.data.presentation.ImageRequest;
import image.module.data.presentation.UpdateImageData;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@RequiredArgsConstructor
@Service
public class ImageService {
    private final ImageRepository imageRepository;

    @Transactional
    public ImageResponse saveImage(ImageRequest request){
        Image image = Image.create(request);
        imageRepository.save(image);
        image.assignOriginalFileUUID();
        return ImageResponse.fromEntity(image);
    }

    public ImageResponse getImageName(UUID id) {
        return ImageResponse.fromEntity(imageRepository.findById(id).orElse(null));
    }

    public ImageResponse getCDNImageName(String cdnUrl) {

        Image image = imageRepository.findByCdnUrl(cdnUrl);

        if(image != null){
            return ImageResponse.fromEntity(image);
        }else {
            //이미지가 없을 경우 처리
            throw new EntityNotFoundException("Image not found"+cdnUrl);
        }

    }

    // size, cdnUrl 업데이트
    public void updateImage(UpdateImageData updateImageData) {

        Image image = imageRepository.findByStoredFileName(updateImageData.getStoredFileName()).orElseThrow(
                () ->  new EntityNotFoundException("저장된 파일 이름을 찾을 수 없습니다")
        );
        image.updateImageData(updateImageData.getSize(), updateImageData.getCdnBaseUrl());
        imageRepository.save(image);
    }


    // 리사이즈 WebP 이미지 DB 저장
    public void creatImage(CreateResizeRequest createResizeRequest) {

        Image image = imageRepository.findByStoredFileName(createResizeRequest.getStoredFileName()).orElseThrow(
                () -> new IllegalArgumentException("저장된 파일 이름을 찾을 수 없습니다")
        );
        Image SaveResizeimage = Image.createResize(image, createResizeRequest);
        imageRepository.save(SaveResizeimage);

    }

}
