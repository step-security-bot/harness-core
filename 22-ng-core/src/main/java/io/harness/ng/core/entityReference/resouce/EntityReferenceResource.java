package io.harness.ng.core.entityReference.resouce;

import com.google.inject.Inject;

import io.harness.ng.core.dto.ResponseDTO;
import io.harness.ng.core.entityReference.dto.EntityReferenceDTO;
import io.harness.ng.core.entityReference.service.EntityReferenceService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.domain.Page;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

@Api("/entityReference")
@Path("entityReference")
@Produces({"application/json", "text/yaml", "text/html"})
@Consumes({"application/json", "text/yaml", "text/html"})
@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
public class EntityReferenceResource {
  EntityReferenceService entityReferenceService;

  @GET
  @ApiOperation(value = "Get Entities referring this resouce", nickname = "getReferredByEntities")
  public ResponseDTO<Page<EntityReferenceDTO>> list(@QueryParam("page") @DefaultValue("0") int page,
      @QueryParam("size") @DefaultValue("100") int size, @NotEmpty @QueryParam("account") String accountIdentifier,
      @QueryParam("org") String orgIdentifier, @QueryParam("project") String projectIdentifier,
      @QueryParam("identifier") String referredEntityIdentifier, @QueryParam("searchTerm") String searchTerm) {
    return ResponseDTO.newResponse(entityReferenceService.list(
        page, size, accountIdentifier, orgIdentifier, projectIdentifier, referredEntityIdentifier, searchTerm));
  }
}
