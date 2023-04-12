/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.releaseradar.resources;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.releaseradar.dto.EventResponseDTO;
import io.harness.releaseradar.entities.EventEntity;
import io.harness.releaseradar.mapper.EventMapper;
import io.harness.repositories.EventRepository;
import io.harness.security.annotations.PublicApi;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import io.swagger.annotations.Api;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import lombok.extern.slf4j.Slf4j;

@Api("views")
@Path("/views")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@PublicApi
@Slf4j
public class ViewResource {
  @Inject private EventRepository eventRepository;

  @GET
  @Path("/")
  public List<EventResponseDTO> filterEvents(@QueryParam("env") String environment, @QueryParam("svc") String svc,
      @QueryParam("release") String release, @QueryParam("version") String version) {
    Stream<EventEntity> eventsStream = Lists.newArrayList(eventRepository.findAll()).stream();
    if (isNotEmpty(environment)) {
      eventsStream = eventsStream.filter(event -> event.getEnvironment().name().equalsIgnoreCase(environment));
    }

    if (isNotEmpty(svc)) {
      eventsStream = eventsStream.filter(event -> event.getServiceName().equalsIgnoreCase(svc));
    }

    if (isNotEmpty(release)) {
      eventsStream = eventsStream.filter(event -> event.getRelease().equalsIgnoreCase(release));
    }

    if (isNotEmpty(version)) {
      eventsStream = eventsStream.filter(event -> event.getBuildVersion().equalsIgnoreCase(version));
    }

    return eventsStream.map(EventMapper::toDTO).collect(Collectors.toList());
  }
}
