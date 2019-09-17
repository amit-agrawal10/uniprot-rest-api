package org.uniprot.api.subcell.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.uniprot.core.cv.subcell.SubcellularLocationEntry;
import org.uniprot.core.json.parser.subcell.SubcellularLocationJsonConfig;
import org.uniprot.store.search.document.subcell.SubcellularLocationDocument;

import java.util.function.Function;

/**
 * @author lgonzales
 * @since 2019-07-19
 */
@Slf4j
public class SubcellularLocationEntryConverter implements Function<SubcellularLocationDocument, SubcellularLocationEntry> {

    private final ObjectMapper objectMapper;

    public SubcellularLocationEntryConverter() {
        objectMapper = SubcellularLocationJsonConfig.getInstance().getFullObjectMapper();
    }

    @Override
    public SubcellularLocationEntry apply(SubcellularLocationDocument subcellularLocationDocument) {
        try {
            return objectMapper.readValue(subcellularLocationDocument.getSubcellularlocationObj().array(), SubcellularLocationEntry.class);
        } catch (Exception e) {
            log.warn("Error converting solr binary to SubcellularLocationEntry: ", e);
        }
        return null;
    }

}
