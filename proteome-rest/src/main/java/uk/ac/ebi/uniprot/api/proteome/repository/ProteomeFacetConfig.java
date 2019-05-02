package uk.ac.ebi.uniprot.api.proteome.repository;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;
import uk.ac.ebi.uniprot.api.common.repository.search.facet.FacetConfigConverter;
import uk.ac.ebi.uniprot.api.common.repository.search.facet.FacetProperty;
import uk.ac.ebi.uniprot.api.common.repository.search.facet.GenericFacetConfig;

/**
 *
 * @author jluo
 * @date: 24 Apr 2019
 *
*/
@Component
@Getter @Setter
@PropertySource("classpath:facet.properties")
@ConfigurationProperties(prefix = "facet")
public class ProteomeFacetConfig extends GenericFacetConfig implements FacetConfigConverter {

	 private Map<String, FacetProperty> proteome = new HashMap<>();
	
	@Override
	public Map<String, FacetProperty> getFacetPropertyMap() {
		return proteome;
	}
	 public Collection<String> getFacetNames() {
	        return proteome.keySet();
	    }
}

