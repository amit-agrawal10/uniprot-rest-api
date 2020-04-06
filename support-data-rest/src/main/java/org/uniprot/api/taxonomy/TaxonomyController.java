package org.uniprot.api.taxonomy;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.uniprot.api.rest.output.UniProtMediaType.*;
import static org.uniprot.api.rest.output.context.MessageConverterContextFactory.Resource.TAXONOMY;

import java.util.Optional;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.rest.controller.BasicSearchController;
import org.uniprot.api.rest.output.context.MessageConverterContext;
import org.uniprot.api.rest.output.context.MessageConverterContextFactory;
import org.uniprot.api.rest.validation.ValidReturnFields;
import org.uniprot.api.taxonomy.request.TaxonomyRequest;
import org.uniprot.api.taxonomy.service.TaxonomyService;
import org.uniprot.core.taxonomy.TaxonomyEntry;
import org.uniprot.core.taxonomy.TaxonomyInactiveReasonType;
import org.uniprot.store.config.UniProtDataType;

@RestController
@RequestMapping("/taxonomy")
@Validated
public class TaxonomyController extends BasicSearchController<TaxonomyEntry> {

    private final TaxonomyService taxonomyService;
    private static final String TAXONOMY_ID_REGEX = "^[0-9]+$";

    public TaxonomyController(
            ApplicationEventPublisher eventPublisher,
            TaxonomyService taxonomyService,
            @Qualifier("taxonomyMessageConverterContextFactory")
                    MessageConverterContextFactory<TaxonomyEntry>
                            taxonomyMessageConverterContextFactory,
            ThreadPoolTaskExecutor downloadTaskExecutor) {
        super(
                eventPublisher,
                taxonomyMessageConverterContextFactory,
                downloadTaskExecutor,
                TAXONOMY);
        this.taxonomyService = taxonomyService;
    }

    @GetMapping(
            value = "/{taxonId}",
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE
            })
    public ResponseEntity<MessageConverterContext<TaxonomyEntry>> getById(
            @PathVariable("taxonId")
                    @Pattern(
                            regexp = TAXONOMY_ID_REGEX,
                            flags = {Pattern.Flag.CASE_INSENSITIVE},
                            message = "{search.taxonomy.invalid.id}")
                    String taxonId,
            @ValidReturnFields(uniProtDataType = UniProtDataType.TAXONOMY)
                    @RequestParam(value = "fields", required = false)
                    String fields,
            HttpServletRequest request) {
        TaxonomyEntry taxonomyEntry = this.taxonomyService.findById(Long.valueOf(taxonId));
        return super.getEntityResponse(taxonomyEntry, fields, request);
    }

    @RequestMapping(
            value = "/search",
            method = RequestMethod.GET,
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE
            })
    public ResponseEntity<MessageConverterContext<TaxonomyEntry>> search(
            @Valid TaxonomyRequest searchRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        QueryResult<TaxonomyEntry> results = taxonomyService.search(searchRequest);
        return super.getSearchResponse(results, searchRequest.getFields(), request, response);
    }

    @RequestMapping(
            value = "/download",
            method = RequestMethod.GET,
            produces = {
                TSV_MEDIA_TYPE_VALUE,
                LIST_MEDIA_TYPE_VALUE,
                APPLICATION_JSON_VALUE,
                XLS_MEDIA_TYPE_VALUE
            })
    public DeferredResult<ResponseEntity<MessageConverterContext<TaxonomyEntry>>> download(
            @Valid TaxonomyRequest searchRequest,
            @RequestHeader(value = "Accept", defaultValue = APPLICATION_JSON_VALUE)
                    MediaType contentType,
            @RequestHeader(value = "Accept-Encoding", required = false) String encoding,
            HttpServletRequest request) {
        Stream<TaxonomyEntry> result = taxonomyService.download(searchRequest);
        return super.download(result, searchRequest.getFields(), contentType, request, encoding);
    }

    @Override
    protected String getEntityId(TaxonomyEntry entity) {
        return String.valueOf(entity.getTaxonId());
    }

    @Override
    protected Optional<String> getEntityRedirectId(TaxonomyEntry entity) {
        if (isInactiveAndMergedEntity(entity)) {
            return Optional.of(String.valueOf(entity.getInactiveReason().getMergedTo()));
        } else {
            return Optional.empty();
        }
    }

    private boolean isInactiveAndMergedEntity(TaxonomyEntry entity) {
        return !entity.isActive()
                && entity.hasInactiveReason()
                && TaxonomyInactiveReasonType.MERGED.equals(
                        entity.getInactiveReason().getInactiveReasonType());
    }
}
