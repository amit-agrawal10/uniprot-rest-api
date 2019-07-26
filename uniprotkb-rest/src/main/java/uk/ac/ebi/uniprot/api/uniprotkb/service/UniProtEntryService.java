package uk.ac.ebi.uniprot.api.uniprotkb.service;

import com.google.common.base.Strings;
import org.springframework.data.domain.Sort;
import org.springframework.data.solr.core.query.Query;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import uk.ac.ebi.uniprot.api.common.exception.ResourceNotFoundException;
import uk.ac.ebi.uniprot.api.common.exception.ServiceException;
import uk.ac.ebi.uniprot.api.common.repository.search.QueryResult;
import uk.ac.ebi.uniprot.api.common.repository.search.SolrRequest;
import uk.ac.ebi.uniprot.api.common.repository.store.StoreStreamer;
import uk.ac.ebi.uniprot.api.uniprotkb.controller.request.FieldsParser;
import uk.ac.ebi.uniprot.api.uniprotkb.controller.request.SearchRequestDTO;
import uk.ac.ebi.uniprot.api.uniprotkb.repository.search.impl.UniProtTermsConfig;
import uk.ac.ebi.uniprot.api.uniprotkb.repository.search.impl.UniprotFacetConfig;
import uk.ac.ebi.uniprot.api.uniprotkb.repository.search.impl.UniprotQueryRepository;
import uk.ac.ebi.uniprot.api.uniprotkb.repository.store.UniProtKBStoreClient;
import uk.ac.ebi.uniprot.domain.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.search.DefaultSearchHandler;
import uk.ac.ebi.uniprot.search.SolrQueryUtil;
import uk.ac.ebi.uniprot.search.document.uniprot.UniProtDocument;
import uk.ac.ebi.uniprot.search.field.UniProtField;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType.TSV_MEDIA_TYPE;
import static uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType.XLS_MEDIA_TYPE;

@Service
public class UniProtEntryService {
    private static final String ACCESSION = "accession_id";
    private final StoreStreamer<UniProtEntry> storeStreamer;
    private final UniProtEntryQueryResultsConverter resultsConverter;
    private final DefaultSearchHandler defaultSearchHandler;
    private final UniProtTermsConfig uniProtTermsConfig;
    private UniprotQueryRepository repository;
    private UniprotFacetConfig uniprotFacetConfig;

    public UniProtEntryService(UniprotQueryRepository repository,
                               UniprotFacetConfig uniprotFacetConfig,
                               UniProtTermsConfig uniProtTermsConfig,
                               UniProtKBStoreClient entryStore,
                               StoreStreamer<UniProtEntry> uniProtEntryStoreStreamer,
                               DefaultSearchHandler defaultSearchHandler) {
        this.repository = repository;
        this.uniProtTermsConfig = uniProtTermsConfig;
        this.defaultSearchHandler = defaultSearchHandler;
        this.uniprotFacetConfig = uniprotFacetConfig;
        this.storeStreamer = uniProtEntryStoreStreamer;
        this.resultsConverter = new UniProtEntryQueryResultsConverter(entryStore);
    }

    public QueryResult<UniProtEntry> search(SearchRequestDTO request) {
        SolrRequest solrRequest = createSolrRequest(request, true);

        QueryResult<UniProtDocument> results = repository
                .searchPage(solrRequest, request.getCursor(), request.getSize());

        return resultsConverter.convertQueryResult(results, FieldsParser.parseForFilters(request.getFields()));
    }

    public UniProtEntry getByAccession(String accession, String fields) {
        try {
            Map<String, List<String>> filters = FieldsParser.parseForFilters(fields);
            SolrRequest solrRequest = SolrRequest.builder().query(ACCESSION + ":" + accession.toUpperCase()).build();
            Optional<UniProtDocument> optionalDoc = repository.getEntry(solrRequest);
            Optional<UniProtEntry> optionalUniProtEntry = optionalDoc
                    .map(doc -> resultsConverter.convertDoc(doc, filters))
                    .orElseThrow(() -> new ResourceNotFoundException("{search.not.found}"));

            return optionalUniProtEntry.orElseThrow(() -> new ResourceNotFoundException("{search.not.found}"));
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            String message = "Could not get accession for: [" + accession + "]";
            throw new ServiceException(message, e);
        }
    }

    public Stream<UniProtEntry> stream(SearchRequestDTO request, MediaType contentType) {
        SolrRequest solrRequest = createSolrRequest(request, false);
        boolean defaultFieldsOnly = FieldsParser
                .isDefaultFilters(FieldsParser.parseForFilters(request.getFields()));
        if (defaultFieldsOnly && (contentType.equals(APPLICATION_JSON) || contentType
                .equals(TSV_MEDIA_TYPE) || contentType.equals(XLS_MEDIA_TYPE))) {
            return storeStreamer.defaultFieldStream(solrRequest);
        } else {
            return storeStreamer.idsToStoreStream(solrRequest);
        }
    }

    public Stream<String> streamIds(SearchRequestDTO request) {
        SolrRequest solrRequest = createSolrRequest(request, false);
        return storeStreamer.idsStream(solrRequest);
    }

    private SolrRequest createSolrRequest(SearchRequestDTO request, boolean includeFacets) {
        SolrRequest.SolrRequestBuilder requestBuilder = SolrRequest.builder();
        String requestedQuery = request.getQuery();

        if (needsToFilterIsoform(request)) {
            requestBuilder.filterQuery(UniProtField.Search.is_isoform.name() + ":" + false);
        }

        if (request.isShowMatchedFields()) {
            requestBuilder.termQuery(requestedQuery);
            uniProtTermsConfig.getFields().forEach(requestBuilder::termField);
        }

        boolean hasScore = false;
        if (defaultSearchHandler.hasDefaultSearch(requestedQuery)) {
            requestedQuery = defaultSearchHandler.optimiseDefaultSearch(requestedQuery);
            hasScore = true;
            requestBuilder.defaultQueryOperator(Query.Operator.OR);
        }
        requestBuilder.query(requestedQuery);
        requestBuilder.sort(getUniProtSort(request.getSort(), hasScore));

        if (includeFacets && request.hasFacets()) {
            requestBuilder.facets(request.getFacetList());
            requestBuilder.facetConfig(uniprotFacetConfig);
        }

        return requestBuilder.build();
    }

    private Sort getUniProtSort(String sortStr, boolean hasScore) {
        if (Strings.isNullOrEmpty(sortStr)) {
            return UniProtSortUtil.createDefaultSort(hasScore);
        } else {
            return UniProtSortUtil.createSort(sortStr);
        }
    }

    /**
     * This method verify if we need to add isoform filter query to remove isoform entries
     * <p>
     * if does not have id fields (we can not filter isoforms when querying for IDS)
     * AND
     * has includeIsoform params in the request URL
     * Then we analyze the includeIsoform request parameter.
     * IMPORTANT: Implementing this way, query search has precedence over isoform request parameter
     *
     * @return true if we need to add isoform filter query
     */
    private boolean needsToFilterIsoform(SearchRequestDTO request) {
        boolean hasIdFieldTerms = SolrQueryUtil.hasFieldTerms(request.getQuery(),
                                                              UniProtField.Search.accession_id.name(),
                                                              UniProtField.Search.mnemonic.name(),
                                                              UniProtField.Search.is_isoform.name());

        if (!hasIdFieldTerms) {
            return !request.isIncludeIsoform();
        } else {
            return false;
        }
    }
}
