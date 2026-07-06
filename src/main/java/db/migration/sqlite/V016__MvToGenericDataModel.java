package db.migration.sqlite;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import com.bensler.decaf.util.entity.EntityReference;
import com.bensler.decaf.util.tree.Hierarchy;
import com.bensler.taggy.persist.DbAccess;
import com.bensler.taggy.persist.Photo;
import com.bensler.taggy.persist.Tag;
import com.bensler.taggy.persist.Thumbnail;
import com.bensler.taggy.persist.v1.V1Blob;
import com.bensler.taggy.persist.v1.V1BlobDbMapper;
import com.bensler.taggy.persist.v1.V1Tag;
import com.bensler.taggy.persist.v1.V1TagDbMapper;
import com.bensler.taggy.persist.v2.DbSetup;
import com.bensler.taggy.persist.v2.ThumbnailDbMapper;
import com.bensler.taggy.persist.v2.V2PhotoDbMapper;
import com.bensler.taggy.persist.v2.V2TagDbMapper;

public class V016__MvToGenericDataModel extends BaseJavaMigration {

  private V1TagDbMapper v1TagDbMapper_;
  private V1BlobDbMapper v1BlobDbMapper_;
  private DbSetup dbSetup_;
  private V2TagDbMapper v2TagDbMapper_;
  private V2PhotoDbMapper v2PhotoDbMapper_;

  private final Map<Integer, Integer> tagV1IdToV2Id_;

  public V016__MvToGenericDataModel() {
    tagV1IdToV2Id_ = new HashMap<>();
  }

  @Override
  public void migrate(Context context) throws Exception {
    final DbAccess dbAccess = new DbAccess(context.getConnection());
    v1TagDbMapper_ = dbAccess.registerMapper(new V1TagDbMapper(dbAccess));
    v1BlobDbMapper_ = dbAccess.registerMapper(new V1BlobDbMapper(dbAccess));
    dbSetup_ = dbAccess.runInTxn2(pCon -> new DbSetup(pCon));
    v2TagDbMapper_ = dbAccess.registerMapper(new V2TagDbMapper(dbAccess, dbSetup_));
    v2PhotoDbMapper_ = dbAccess.registerMapper(new V2PhotoDbMapper(
      dbAccess.registerMapper(new ThumbnailDbMapper(dbAccess, dbSetup_)), dbAccess, dbSetup_
    ));

    final Hierarchy<V1Tag> oldTags = new Hierarchy<>(dbAccess.loadAll(V1Tag.class));

    mapTagsAndPersist(oldTags, oldTags.getRoots());
    mapPhotosAndPersist(v1BlobDbMapper_.loadAllEntities(List.of()));

    System.out.println(oldTags.size());
  }

  private void mapPhotosAndPersist(List<V1Blob> oldBlobs) throws SQLException {
    final ThumbnailDbMapper thumbnailDbMapper = v2PhotoDbMapper_.getThumbnailDbMapper();

    for (V1Blob v1Blob : oldBlobs) {
      final Integer newThumbId = thumbnailDbMapper.insert(new Thumbnail(null, v1Blob.getThumbnailSha()));
      final Thumbnail newThumb = thumbnailDbMapper.loadAllEntities(List.of(newThumbId)).get(0);
      final Set<EntityReference<Tag>> tags = v1Blob.getTagRefs().stream()
        .map(v1TagRef -> tagV1IdToV2Id_.computeIfAbsent(v1TagRef.getId(), missingV1TagId -> {
          throw new IllegalStateException("missingV1TagId: " + missingV1TagId);
        }))
        .map(v2TagId -> new EntityReference<>(Tag.class, v2TagId))
        .collect(Collectors.toSet());
      final Integer newPhotoId = v2PhotoDbMapper_.insert(
        new Photo(null, v1Blob.getSha256sum(), newThumb, v1Blob.getType(), v1Blob.getMetaData(), tags)
      );

      System.out.println("newPhotoId: " + newPhotoId);
    }

  }

  private void mapTagsAndPersist(Hierarchy<V1Tag> allOldTags, Set<V1Tag> tags) {
    for (V1Tag v1Tag : tags) {
      final Set<V1Tag> children;
      final Integer newTagId = v2TagDbMapper_.insert(new Tag(
        v1Tag.getParentRef().map(v1ParentRef -> new EntityReference<>(Tag.class, tagV1IdToV2Id_.get(v1ParentRef.getId()))),
        v1Tag.getName(), v1Tag.getProperties()
      ));

      System.out.println("newTagId: " + newTagId);
      tagV1IdToV2Id_.put(v1Tag.getId(), newTagId);
      children = allOldTags.getChildren(v1Tag);
      if (!children.isEmpty()) {
        mapTagsAndPersist(allOldTags, children);
      }
    }
  }

}
