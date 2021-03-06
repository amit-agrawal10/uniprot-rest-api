package org.uniprot.api.uniparc.service.filter;

import static org.uniprot.core.uniparc.UniParcCrossReference.*;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.uniprot.core.Property;
import org.uniprot.core.uniparc.UniParcCrossReference;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.uniparc.impl.UniParcEntryBuilder;
import org.uniprot.core.util.Utils;

/**
 * @author lgonzales
 * @since 14/08/2020
 */
public class UniParcCrossReferenceTaxonomyFilter
        implements BiFunction<UniParcEntry, List<String>, UniParcEntry> {

    @Override
    public UniParcEntry apply(UniParcEntry uniParcEntry, List<String> taxonomyIds) {
        UniParcEntryBuilder builder = UniParcEntryBuilder.from(uniParcEntry);
        List<UniParcCrossReference> xrefs = uniParcEntry.getUniParcCrossReferences();
        if (Utils.notNullNotEmpty(xrefs) && Utils.notNullNotEmpty(taxonomyIds)) {
            List<UniParcCrossReference> filteredRefs =
                    xrefs.stream()
                            .filter(xref -> Objects.nonNull(xref.getProperties()))
                            .filter(xref -> taxonomyIds.contains(getTaxIdFromXRef(xref)))
                            .collect(Collectors.toList());
            builder.uniParcCrossReferencesSet(filteredRefs);
        }
        return builder.build();
    }

    private String getTaxIdFromXRef(UniParcCrossReference xref) {
        return xref.getProperties().stream()
                .filter(property -> property.getKey().equalsIgnoreCase(PROPERTY_NCBI_TAXONOMY_ID))
                .map(Property::getValue)
                .findFirst()
                .orElse("");
    }
}
