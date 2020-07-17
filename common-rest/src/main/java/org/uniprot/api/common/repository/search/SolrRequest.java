package org.uniprot.api.common.repository.search;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import org.apache.solr.client.solrj.SolrQuery;
import org.uniprot.api.common.repository.search.facet.FacetConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a request object containing the details to create a query to send to Solr.
 *
 * @author Edd
 */
@Data
@Builder(builderClassName = "SolrRequestBuilder", toBuilder = true)
public class SolrRequest {
    // TODO: 17/07/2020 should this be and or or and, after using {@link DefaultSearchOptimiser}?
    // use case: X OR Y => X Y. With default operator AND => X AND Y => wrong result.
    // If change default operator to OR
    // use case: X Y => X Y. With default operator OR => X OR Y => wrong result.
    // conclusion -- 2 query parsers with their own default operator causes problem when
    // manipulating query. How to fix this?
    private static final QueryOperator DEFAULT_OPERATOR = QueryOperator.AND;
    private String query;
    private QueryOperator defaultQueryOperator;
    private FacetConfig facetConfig;
    private String termQuery;
    private QueryBoosts queryBoosts;
    private String defaultField;
    // Batch size of rows in solr request. In case of search api request rows and totalRows will be
    // same.
    private int rows;
    // Total rows requested by user
    private int totalRows;

    @Singular private List<String> termFields = new ArrayList<>();
    @Singular private List<String> filterQueries = new ArrayList<>();
    @Singular private List<String> facets = new ArrayList<>();
    @Singular private List<SolrQuery.SortClause> sorts = new ArrayList<>();

    // setting default field values in a builder following instructions here:
    // https://www.baeldung.com/lombok-builder-default-value
    public static class SolrRequestBuilder {
        private QueryOperator defaultQueryOperator = DEFAULT_OPERATOR;
    }
}
