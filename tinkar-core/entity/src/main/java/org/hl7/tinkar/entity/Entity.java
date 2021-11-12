package org.hl7.tinkar.entity;

import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.primitive.MutableIntList;
import org.eclipse.collections.impl.factory.primitive.IntLists;
import org.hl7.tinkar.common.id.IntIdSet;
import org.hl7.tinkar.common.id.IntIds;
import org.hl7.tinkar.common.id.PublicId;
import org.hl7.tinkar.common.service.PrimitiveData;
import org.hl7.tinkar.component.Chronology;
import org.hl7.tinkar.component.Component;
import org.hl7.tinkar.component.FieldDataType;
import org.hl7.tinkar.terms.EntityFacade;
import org.hl7.tinkar.terms.SemanticFacade;

import java.util.Arrays;
import java.util.Optional;

public interface Entity<T extends EntityVersion>
        extends Chronology<T>,
        EntityFacade,
        IdentifierData {

    static EntityService provider() {
        return EntityService.get();
    }

    static int nid(Component component) {
        return EntityService.get().nidForComponent(component);
    }

    static Optional<ConceptEntity> getConceptForSemantic(SemanticFacade semanticFacade) {
        return getConceptForSemantic(semanticFacade.nid());
    }

    static Optional<ConceptEntity> getConceptForSemantic(int semanticNid) {
        Optional<? extends Entity<? extends EntityVersion>> optionalEntity = get(semanticNid);
        if (optionalEntity.isPresent()) {
            if (optionalEntity.get() instanceof SemanticEntity semanticEntity) {
                Entity<?> referencedEntity = Entity.getFast(semanticEntity.referencedComponentNid());
                if (referencedEntity instanceof ConceptEntity conceptEntity) {
                    return Optional.of(conceptEntity);
                } else if (referencedEntity instanceof SemanticEntity referencedSemantic) {
                    return getConceptForSemantic(referencedSemantic);
                }
            }
        }
        return Optional.empty();
    }

    static <T extends Entity<V>, V extends EntityVersion> Optional<T> get(int nid) {
        return EntityService.get().getEntity(nid);
    }

    static <T extends Entity<V>, V extends EntityVersion> Optional<T> get(EntityFacade facade) {
        return EntityService.get().getEntity(facade.nid());
    }

    static <T extends Entity<V>, V extends EntityVersion> T getFast(int nid) {
        return EntityService.get().getEntityFast(nid);
    }

    static <T extends Entity<V>, V extends EntityVersion> T getFast(EntityFacade facade) {
        return EntityService.get().getEntityFast(facade.nid());
    }

    static <T extends StampEntity<? extends StampEntityVersion>> T getStamp(int nid) {
        return EntityService.get().getStampFast(nid);
    }

    default IntIdSet stampNids() {
        MutableIntList stampNids = IntLists.mutable.withInitialCapacity(versions().size());
        for (EntityVersion version : versions()) {
            stampNids.add(version.stampNid());
        }
        return IntIds.set.of(stampNids.toArray());
    }

    ;

    @Override
    ImmutableList<T> versions();

    byte[] getBytes();

    FieldDataType entityDataType();

    FieldDataType versionDataType();

    default String entityToString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getSimpleName());
        sb.append("{");
        Optional<String> stringOptional = PrimitiveData.textOptional(this.nid());
        if (stringOptional.isPresent()) {
            sb.append(stringOptional.get());
            sb.append(' ');
        }
        sb.append("<");
        sb.append(nid());
        sb.append("> ");
        sb.append(Arrays.toString(publicId().asUuidArray()));
        sb.append(", ");
        sb.append(entityToStringExtras());
        for (EntityVersion version : versions()) {
            sb.append("\nv: ").append(version).append(",");
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append('}');
        return sb.toString();
    }

    int nid();

    @Override
    default PublicId publicId() {
        return IdentifierData.super.publicId();
    }

    default String entityToStringExtras() {
        return "";
    }

    /**
     * @return true if all versions of entity are canceled.
     */
    default boolean canceled() {
        for (EntityVersion v : versions()) {
            if (!v.canceled()) {
                return false;
            }
        }
        return true;
    }

}
