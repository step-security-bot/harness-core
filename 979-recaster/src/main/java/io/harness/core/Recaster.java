/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.core;
import static java.lang.String.format;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.CodePulse;
import io.harness.annotations.dev.HarnessModuleComponent;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.ProductModule;
import io.harness.beans.CastedClass;
import io.harness.beans.CastedField;
import io.harness.beans.RecasterMap;
import io.harness.exception.CastedFieldException;
import io.harness.exceptions.RecasterException;
import io.harness.fieldrecaster.ComplexFieldRecaster;
import io.harness.fieldrecaster.FieldRecaster;
import io.harness.fieldrecaster.SimpleValueFieldRecaster;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@CodePulse(module = ProductModule.CDS, unitCoverageRequired = true, components = {HarnessModuleComponent.CDS_PIPELINE})
@OwnedBy(HarnessTeam.PIPELINE)
@Getter
@Slf4j
public class Recaster {
  public static final String RECAST_CLASS_KEY = "__recast";
  public static final String ENCODED_VALUE = "__encodedValue";

  private final Map<String, CastedClass> castedClasses = new ConcurrentHashMap<>();
  private final Transformer transformer;
  private final FieldRecaster defaultFieldRecaster;
  private final FieldRecaster simpleValueFieldRecaster;
  private final RecastObjectFactory objectFactory = new RecastObjectCreator();

  private RecasterOptions options;

  public Recaster() {
    this.options = RecasterOptions.builder().build();
    this.defaultFieldRecaster = new ComplexFieldRecaster();
    this.simpleValueFieldRecaster = new SimpleValueFieldRecaster();

    this.transformer = new CustomTransformer(this);
  }

  public Recaster(RecasterOptions recasterOptions) {
    this.options = recasterOptions;
    this.defaultFieldRecaster = new ComplexFieldRecaster();
    this.simpleValueFieldRecaster = new SimpleValueFieldRecaster();

    this.transformer = new CustomTransformer(this);
  }

  public boolean isCasted(Class<?> entityClass) {
    return castedClasses.containsKey(entityClass.getName());
  }

  public CastedClass addCastedClass(Class<?> entityClass) {
    CastedClass castedClass = castedClasses.get(entityClass.getName());
    if (castedClass == null) {
      castedClass = new CastedClass(entityClass, this);
      castedClasses.put(castedClass.getClazz().getName(), castedClass);
    }
    return castedClass;
  }

  public CastedClass getCastedClass(Object obj) {
    if (obj == null) {
      return null;
    }
    Class<?> type = (obj instanceof Class) ? (Class<?>) obj : obj.getClass();
    CastedClass cc = castedClasses.get(type.getName());
    if (cc == null) {
      cc = new CastedClass(type, this);
      castedClasses.put(cc.getClazz().getName(), cc);
    }
    return cc;
  }

  public <T> T fromMap(final Map<String, Object> map, final Class<T> entityClazz) {
    return fromMap(map, entityClazz, false);
  }

  public <T> T fromMap(final Map<String, Object> map, final Class<T> entityClazz, boolean newRecastFlow) {
    if (map == null) {
      return null;
    }

    return fromMap(new RecasterMap(map), entityClazz, newRecastFlow);
  }

  public <T> T fromMap(final RecasterMap recasterMap, final Class<T> entityClazz, boolean newRecastFlow) {
    if (recasterMap == null) {
      return null;
    }

    Object classIdentifier = recasterMap.getIdentifier();

    if (classIdentifier == null) {
      throw new RecasterException(format(
          "The document does not contain any identifiers %s. Determining entity type is impossible. Consider adding %s annotation to your class",
          RECAST_CLASS_KEY, RecasterAlias.class.getSimpleName()));
    }

    T entity;
    entity = objectFactory.createInstance(entityClazz, recasterMap);

    // Add handling to try for class instance of recasterMap.getEncodedValue as class __recaster gave an exception
    // This will work out for cases like primitives and String arrays instance creation and encodedValue is different.
    // [a, b, c] -> even though its String[] but in encoded value its coming as ArrayList, due to
    // JsonUtils.asMap(json);
    if (entity == null) {
      entity = (T) objectFactory.createGivenInstance(recasterMap);
    }

    if (!entityClazz.isAssignableFrom(entity.getClass())) {
      throw new RecasterException(format("%s class cannot be mapped to %s", classIdentifier, entityClazz.getName()));
    }

    entity = fromMap(recasterMap, entity, newRecastFlow);
    return entity;
  }

  public <T> T fromMap(final RecasterMap recasterMap, T entity) {
    return fromMap(recasterMap, entity, false);
  }

  @SuppressWarnings("unchecked")
  public <T> T fromMap(final RecasterMap recasterMap, T entity, boolean newRecastFlow) {
    if (transformer.hasCustomTransformer(entity.getClass())) {
      entity = (T) transformer.decode(entity.getClass(), recasterMap, null);
    } else if (entity instanceof Map) {
      populateMapInternal(recasterMap, entity);
    } else if (entity instanceof Collection) {
      populateCollectionInternal(recasterMap, entity);
    } else if (newRecastFlow && transformer.hasSimpleValueTransformer(entity.getClass())) {
      entity = (T) transformer.decode(entity.getClass(), recasterMap, null);
    } else {
      final CastedClass castedClass = getCastedClass(entity);
      for (final CastedField cf : castedClass.getPersistenceFields()) {
        try {
          readCastedField(recasterMap, cf, entity);
        } catch (final Exception e) {
          throwErrorBasedOnException(
              e, (String) recasterMap.getIdentifier(), entity.getClass().getSimpleName(), cf.getField().getName());
        }
      }
    }

    return entity;
  }

  private void throwErrorBasedOnException(
      Exception e, String actualClassType, String expectedClassType, String fieldName) {
    if (e instanceof RecasterException || e instanceof CastedFieldException) {
      String fieldPath = e instanceof RecasterException ? ((RecasterException) e).getFieldPath()
                                                        : ((CastedFieldException) e).getFieldPath();
      String messageWithoutFieldPath = e instanceof RecasterException
          ? ((RecasterException) e).getMessageWithoutFieldPath()
          : ((CastedFieldException) e).getMessageWithoutFieldPath();
      throw new CastedFieldException(e.getMessage(), e, fieldPath, messageWithoutFieldPath);
    } else {
      throw new CastedFieldException(
          String.format("Cannot map [%s] to [%s] class for field [%s]", actualClassType, expectedClassType, fieldName),
          e);
    }
  }

  @SuppressWarnings("unchecked")
  private <T> void populateMapInternal(final RecasterMap recasterMap, T entity) {
    Map<String, Object> entityMap = (Map<String, Object>) entity;
    for (Map.Entry<String, Object> entry : recasterMap.entrySet()) {
      if (entry.getKey().equals(RECAST_CLASS_KEY)) {
        continue;
      }
      Object o = recasterMap.get(entry.getKey());
      try {
        entityMap.put(entry.getKey(), (o instanceof Map) ? fromMap(RecasterMap.cast((Map<String, Object>) o)) : o);
      } catch (RecasterException ex) {
        log.warn(
            String.format(
                "RecasterKey not present in the RecasterMap for KEY %s. So putting the original recasterMap as the value inside EntityMap",
                entry.getKey()),
            ex);
        entityMap.put(entry.getKey(), o);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private <T> void populateCollectionInternal(final RecasterMap recasterMap, T entity) {
    if (!recasterMap.containsEncodedValue()) {
      return;
    }
    Collection<Object> collection = (Collection<Object>) entity;
    for (Object o : (List<Object>) recasterMap.getEncodedValue()) {
      collection.add((o instanceof Map) ? fromMap(RecasterMap.cast((Map<String, Object>) o)) : o);
    }
  }

  private <T> T fromMap(RecasterMap recasterMap) {
    if (recasterMap.containsIdentifier()) {
      T entity = getObjectFactory().createInstance(null, recasterMap);
      entity = fromMap(recasterMap, entity);

      return entity;
    } else {
      throw new RecasterException(format(
          "The document does not contain any identifiers %s. Determining entity type is impossible. Consider adding %s annotation to your class",
          RECAST_CLASS_KEY, RecasterAlias.class.getSimpleName()));
    }
  }

  private void readCastedField(final RecasterMap recasterMap, final CastedField cf, final Object entity) {
    // Annotation logic should be wired here
    if (transformer.hasSimpleValueTransformer(cf.getType())) {
      simpleValueFieldRecaster.fromMap(this, recasterMap, cf, entity);
    } else {
      defaultFieldRecaster.fromMap(this, recasterMap, cf, entity);
    }
  }

  public Map<String, Object> toMap(Object entity) {
    return toMap(entity, false);
  }

  public Map<String, Object> toMap(Object entity, boolean newRecastFlow) {
    if (entity == null) {
      return null;
    }

    final RecasterMap recasterMap = new RecasterMap();
    final CastedClass cc = getCastedClass(entity);

    recasterMap.setIdentifier(entity.getClass());

    if (transformer.hasCustomTransformer(entity.getClass())) {
      Object encode = transformer.encode(entity);
      return recasterMap.append(ENCODED_VALUE, encode);
    }

    if (entity instanceof Map) {
      return writeMapInternal(recasterMap, entity);
    }

    if (entity instanceof Collection) {
      return writeCollectionInternal(recasterMap, entity);
    }

    if (entity.getClass().isArray()) {
      return writeArrayInternal(recasterMap, entity);
    }

    if (transformer.hasSimpleValueTransformer(entity.getClass())) {
      if (!newRecastFlow) {
        log.warn("[RECAST_NEW_FLOW]: Inside Simple Transformer for entity class: " + entity.getClass());
      } else {
        Object encode = transformer.encode(entity);
        return recasterMap.append(ENCODED_VALUE, encode);
      }
    }

    for (final CastedField cf : cc.getPersistenceFields()) {
      try {
        writeCastedField(entity, cf, recasterMap);
      } catch (Exception e) {
        throw new CastedFieldException(format("Cannot map [%s] to [%s] class for field [%s]",
                                           recasterMap.getIdentifier(), entity.getClass(), cf.getField().getName()),
            e);
      }
    }

    return recasterMap;
  }

  private Map<String, Object> writeMapInternal(RecasterMap recasterMap, Object entity) {
    Object encoded = transformer.getTransformer(entity.getClass()).encode(entity);
    if (encoded == null) {
      return recasterMap;
    }
    if (!(encoded instanceof Map)) {
      throw new RecasterException(format("Cannot transform %s to map", entity.getClass()));
    }

    recasterMap.putAll((Map) encoded);
    return recasterMap;
  }

  private Map<String, Object> writeCollectionInternal(RecasterMap recasterMap, Object entity) {
    Collection<Object> encoded = (Collection<Object>) transformer.getTransformer(entity.getClass()).encode(entity);
    if (encoded == null) {
      return recasterMap;
    }
    recasterMap.setEncodedValue(encoded);
    return recasterMap;
  }

  private Map<String, Object> writeArrayInternal(RecasterMap recasterMap, Object entity) {
    Object encoded = transformer.getTransformer(entity.getClass()).encode(entity);
    if (encoded == null) {
      return recasterMap;
    }
    recasterMap.setEncodedValue(encoded);
    return recasterMap;
  }

  private void writeCastedField(Object entity, CastedField cf, RecasterMap recasterMap) {
    // Annotation logic should be wired here
    if (transformer.hasSimpleValueTransformer(cf.getType())) {
      simpleValueFieldRecaster.toMap(this, entity, cf, recasterMap);
    } else {
      defaultFieldRecaster.toMap(this, entity, cf, recasterMap);
    }
  }
}
