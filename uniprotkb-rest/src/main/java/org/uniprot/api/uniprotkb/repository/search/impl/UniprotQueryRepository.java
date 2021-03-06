package org.uniprot.api.uniprotkb.repository.search.impl;

import org.apache.solr.client.solrj.SolrClient;
import org.springframework.stereotype.Repository;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.document.uniprot.UniProtDocument;

/**
 * Repository responsible to query SolrCollection.uniprot
 *
 * @author lgonzales
 */
@Repository
public class UniprotQueryRepository extends SolrQueryRepository<UniProtDocument> {
    public UniprotQueryRepository(
            SolrClient solrClient,
            UniprotKBFacetConfig facetConfig,
            SolrRequestConverter requestConverter) {
        super(
                solrClient,
                SolrCollection.uniprot,
                UniProtDocument.class,
                facetConfig,
                requestConverter);
    }
}
