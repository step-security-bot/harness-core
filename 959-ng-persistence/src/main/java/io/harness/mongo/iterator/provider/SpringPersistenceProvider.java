/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.mongo.iterator.provider;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.govern.Switch.unhandled;

import static java.lang.System.currentTimeMillis;

import io.harness.annotations.dev.OwnedBy;
import io.harness.iterator.PersistentIterable;
import io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType;
import io.harness.mongo.iterator.filter.SpringFilterExpander;

import com.mongodb.BasicDBObject;
import java.time.Duration;
import java.util.List;
import org.mongodb.morphia.query.FilterOperator;
import org.mongodb.morphia.query.MorphiaIterator;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

@OwnedBy(CDC)
public class SpringPersistenceProvider<T extends PersistentIterable>
    implements PersistenceProvider<T, SpringFilterExpander> {
  private final MongoTemplate persistence;

  public SpringPersistenceProvider(MongoTemplate persistence) {
    this.persistence = persistence;
  }

  private Query createQuery(String fieldName, SpringFilterExpander filterExpander, boolean unsorted) {
    Query query = new Query();
    if (!unsorted) {
      query.with(Sort.by(new Order(Sort.Direction.ASC, fieldName)));
    }
    if (filterExpander != null) {
      filterExpander.filter(query);
    }
    return query;
  }

  private Query createQuery(long now, String fieldName, SpringFilterExpander filterExpander, boolean unsorted) {
    Criteria criteria = new Criteria();
    Query query = createQuery(fieldName, filterExpander, unsorted);
    if (filterExpander == null) {
      query.addCriteria(
          criteria.orOperator(Criteria.where(fieldName).lt(now), Criteria.where(fieldName).exists(false)));
    } else {
      query.addCriteria(criteria.andOperator(
          criteria.orOperator(Criteria.where(fieldName).lt(now), Criteria.where(fieldName).exists(false))));
    }
    return query;
  }

  @Override
  public void updateEntityField(T entity, List<Long> nextIterations, Class<T> clazz, String fieldName) {
    Update update = new Update();
    update.set(fieldName, nextIterations);
    persistence.updateFirst(new Query(Criteria.where("_id").is(entity.getUuid())), update, clazz);
  }

  @Override
  public T obtainNextInstance(long base, long throttled, Class<T> clazz, String fieldName,
      SchedulingType schedulingType, Duration targetInterval, SpringFilterExpander filterExpander, boolean unsorted) {
    long now = currentTimeMillis();
    Query query = createQuery(now, fieldName, filterExpander, unsorted);
    Update update = new Update();
    switch (schedulingType) {
      case REGULAR:
        update.set(fieldName, base + targetInterval.toMillis());
        break;
      case IRREGULAR:
        update.pop(fieldName, Update.Position.FIRST);
        break;
      case IRREGULAR_SKIP_MISSED:
        update.pull(fieldName, new BasicDBObject(FilterOperator.LESS_THAN_OR_EQUAL.val(), throttled));
        break;
      default:
        unhandled(schedulingType);
    }
    return persistence.findAndModify(
        query, update, FindAndModifyOptions.options().upsert(false).returnNew(false), clazz);
  }

  @Override
  public T findInstance(Class<T> clazz, String fieldName, SpringFilterExpander filterExpander) {
    return persistence.findOne(createQuery(fieldName, filterExpander, false), clazz);
  }

  @Override
  public void recoverAfterPause(Class<T> clazz, String fieldName) {
    Update updateNull = new Update();
    updateNull.unset(fieldName);
    persistence.updateFirst(new Query(Criteria.where(fieldName).is(null)), updateNull, clazz);
    Update updateEmpty = new Update();
    updateEmpty.unset(fieldName);
    persistence.updateFirst(new Query(Criteria.where(fieldName).size(0)), updateEmpty, clazz);
  }

  @Override
  public long getDocumentsCount(Class<T> clazz) {
    return 0;
  }

  @Override
  public T getOneDocumentBySkip(Class<T> clazz, SpringFilterExpander filterExpander, int limit) {
    return null;
  }

  @Override
  public MorphiaIterator<T, T> getDocumentsGreaterThanID(
      Class<T> clazz, SpringFilterExpander filterExpander, String id, int limit) {
    return null;
  }
}
