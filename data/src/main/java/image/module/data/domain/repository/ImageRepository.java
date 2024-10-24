package image.module.data.domain.repository;

import image.module.data.domain.Image;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ImageRepository extends JpaRepository<Image, UUID> {
    Image findByCdnUrl(String cdnUrl);
  Optional<Image> findByStoredFileName(String storedFileName);

    @Query("SELECT i FROM image i WHERE i.originalFileUUID = :originalFileUuid AND i.size = :size")
    Optional<Image> findByOriginalFileUuidAndSize(@Param("originalFileUuid") UUID originalFileUuid,
                                                  @Param("size") Integer size);
}
