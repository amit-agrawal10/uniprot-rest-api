package uk.ac.ebi.uniprot.api.uniparc.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.core.JsonProcessingException;

import uk.ac.ebi.uniprot.api.rest.controller.AbstractGetByIdControllerIT;
import uk.ac.ebi.uniprot.api.rest.controller.param.ContentTypeParam;
import uk.ac.ebi.uniprot.api.rest.controller.param.GetIdContentTypeParam;
import uk.ac.ebi.uniprot.api.rest.controller.param.GetIdParameter;
import uk.ac.ebi.uniprot.api.rest.controller.param.resolver.AbstractGetIdContentTypeParamResolver;
import uk.ac.ebi.uniprot.api.rest.controller.param.resolver.AbstractGetIdParameterResolver;
import uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType;
import uk.ac.ebi.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import uk.ac.ebi.uniprot.api.uniparc.UniParcRestApplication;
import uk.ac.ebi.uniprot.domain.Location;
import uk.ac.ebi.uniprot.domain.Property;
import uk.ac.ebi.uniprot.domain.Sequence;
import uk.ac.ebi.uniprot.domain.builder.SequenceBuilder;
import uk.ac.ebi.uniprot.domain.uniparc.InterproGroup;
import uk.ac.ebi.uniprot.domain.uniparc.SequenceFeature;
import uk.ac.ebi.uniprot.domain.uniparc.SignatureDbType;
import uk.ac.ebi.uniprot.domain.uniparc.UniParcDBCrossReference;
import uk.ac.ebi.uniprot.domain.uniparc.UniParcDatabaseType;
import uk.ac.ebi.uniprot.domain.uniparc.UniParcEntry;
import uk.ac.ebi.uniprot.domain.uniparc.builder.InterProGroupBuilder;
import uk.ac.ebi.uniprot.domain.uniparc.builder.SequenceFeatureBuilder;
import uk.ac.ebi.uniprot.domain.uniparc.builder.UniParcDBCrossReferenceBuilder;
import uk.ac.ebi.uniprot.domain.uniparc.builder.UniParcEntryBuilder;
import uk.ac.ebi.uniprot.domain.uniparc.builder.UniParcIdBuilder;
import uk.ac.ebi.uniprot.domain.uniprot.taxonomy.Taxonomy;
import uk.ac.ebi.uniprot.domain.uniprot.taxonomy.builder.TaxonomyBuilder;
import uk.ac.ebi.uniprot.indexer.DataStoreManager;
import uk.ac.ebi.uniprot.json.parser.uniparc.UniParcJsonConfig;
import uk.ac.ebi.uniprot.search.document.uniparc.UniParcDocument;
import uk.ac.ebi.uniprot.search.document.uniparc.UniParcDocument.UniParcDocumentBuilder;

/**
 *
 * @author jluo
 * @date: 25 Jun 2019
 *
*/
@ContextConfiguration(classes= {UniParcDataStoreTestConfig.class, UniParcRestApplication.class, ErrorHandlerConfig.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(UniParcController.class)
@ExtendWith(value = {SpringExtension.class, UniParcGetIdControllerIT.UniParcGetIdParameterResolver.class,
		UniParcGetIdControllerIT.UniParcGetIdContentTypeParamResolver.class})
public class UniParcGetIdControllerIT extends AbstractGetByIdControllerIT {
	 private static final String UPI = "UPI0000083A08";
	    @Autowired
	    private MockMvc mockMvc;

	    @Autowired
	    private DataStoreManager storeManager;
		@Override
		protected void saveEntry() {
			UniParcEntry entry = create();
			UniParcDocumentBuilder builder = UniParcDocument.builder();
			builder.upi(UPI).seqLength(entry.getSequence().getLength())
					.sequenceChecksum(entry.getSequence().getCrc64());
			entry.getDbXReferences().forEach(val -> processDbReference(val, builder));
			builder.entryStored(getBinary(entry));
			entry.getTaxonomies().stream().forEach(taxon -> processTaxonomy(taxon, builder));
			
			UniParcDocument doc =builder.build();
			
			  storeManager.saveDocs(DataStoreManager.StoreType.UNIPARC, doc);

		}
		
		
		private void processDbReference(UniParcDBCrossReference xref, UniParcDocumentBuilder builder) {
			UniParcDatabaseType type = xref.getDatabaseType();
			if (xref.isActive()) {
				builder.active(type.toDisplayName());
			}
			builder.database(type.toDisplayName());
			if ((type == UniParcDatabaseType.SWISSPROT) || (type == UniParcDatabaseType.TREMBL)) {
				builder.uniprotAccession(xref.getId());
				builder.uniprotIsoform(xref.getId());
			}

			if (type == UniParcDatabaseType.SWISSPROT_VARSPLIC) {
				builder.uniprotIsoform(xref.getId());
			}
			xref.getProperties().stream().filter(val -> val.getKey().equals(UniParcDBCrossReference.PROPERTY_PROTEOME_ID))
					.map(val -> val.getValue()).forEach(val -> builder.upid(val));

			xref.getProperties().stream().filter(val -> val.getKey().equals(UniParcDBCrossReference.PROPERTY_PROTEIN_NAME))
					.map(val -> val.getValue()).forEach(val -> builder.proteinName(val));

			xref.getProperties().stream().filter(val -> val.getKey().equals(UniParcDBCrossReference.PROPERTY_GENE_NAME))
					.map(val -> val.getValue()).forEach(val -> builder.geneName(val));

		}

		private void processTaxonomy(Taxonomy taxon, UniParcDocumentBuilder builder) {
			builder.taxLineageId((int) taxon.getTaxonId());
			builder.organismTaxon(taxon.getScientificName());
			if (taxon.hasCommonName()) {
				builder.organismTaxon(taxon.getCommonName());
			}
			if (taxon.hasMnemonic()) {
				builder.organismTaxon(taxon.getMnemonic());
			}
			if (taxon.hasSynonyms()) {
				builder.organismTaxons(taxon.getSynonyms());
			}
		}

		 private ByteBuffer getBinary(UniParcEntry entry) {
		        try {
		            return ByteBuffer.wrap(UniParcJsonConfig.getInstance().getFullObjectMapper().writeValueAsBytes(entry));
		        } catch (JsonProcessingException e) {
		            throw new RuntimeException("Unable to parse TaxonomyEntry to binary json: ", e);
		        }
		    }

		
		
		@Override
		protected MockMvc getMockMvc() {
			return mockMvc;
		}

		@Override
		protected String getIdRequestPath() {
			return  "/uniparc/";
		}
		private UniParcEntry create() {
			String seq = "MVSWGRFICLVVVTMATLSLARPSFSLVED";
			Sequence sequence = new SequenceBuilder(seq).build();
			List<UniParcDBCrossReference> xrefs = getXrefs();
			List<SequenceFeature> seqFeatures = getSeqFeatures() ;
			List<Taxonomy> taxonomies =getTaxonomies();
			UniParcEntry entry = new UniParcEntryBuilder().uniParcId(new UniParcIdBuilder(UPI).build())
					.databaseCrossReferences(xrefs).sequence(sequence)
					.sequenceFeatures(seqFeatures)
					.taxonomies(taxonomies).build();
			return entry;
		}
		private List<Taxonomy> getTaxonomies(){
			Taxonomy taxonomy = TaxonomyBuilder.newInstance().taxonId(9606).scientificName("Homo sapiens").build();
			Taxonomy taxonomy2 = TaxonomyBuilder.newInstance().taxonId(10090).scientificName("MOUSE").build();
			return Arrays.asList(taxonomy, taxonomy2);
		}
		private List<SequenceFeature> getSeqFeatures() {
			List<Location> locations = Arrays.asList(new Location(12, 23), new Location(45, 89));
			InterproGroup domain = new InterProGroupBuilder().name("name1").id("id1").build();
			SequenceFeature sf = new SequenceFeatureBuilder().interproGroup(domain).signatureDbType(SignatureDbType.PFAM)
					.signatureDbId("sigId2").locations(locations).build();
			SequenceFeature sf3 = new SequenceFeatureBuilder().from(sf).signatureDbType(SignatureDbType.PROSITE).build();
			return Arrays.asList(sf, sf3);
		}

		private List<UniParcDBCrossReference> getXrefs() {
			List<Property> properties = new ArrayList<>();
			properties.add(new Property(UniParcDBCrossReference.PROPERTY_PROTEIN_NAME, "some pname"));
			properties.add(new Property(UniParcDBCrossReference.PROPERTY_GENE_NAME, "some gname"));
			UniParcDBCrossReference xref = new UniParcDBCrossReferenceBuilder().versionI(3)
					.databaseType(UniParcDatabaseType.SWISSPROT).id("P12345").version(7).active(true)
					.created(LocalDate.of(2017, 5, 17)).lastUpdated(LocalDate.of(2017, 2, 27)).properties(properties)
					.build();

			List<Property> properties2 = new ArrayList<>();
			properties2.add(new Property(UniParcDBCrossReference.PROPERTY_PROTEIN_NAME, "some pname"));
			properties2.add(new Property(UniParcDBCrossReference.PROPERTY_NCBI_TAXONOMY_ID, "9606"));

			UniParcDBCrossReference xref2 = new UniParcDBCrossReferenceBuilder().versionI(1)
					.databaseType(UniParcDatabaseType.TREMBL).id("P52346").version(7).active(true)
					.created(LocalDate.of(2017, 2, 12)).lastUpdated(LocalDate.of(2017, 4, 23)).properties(properties2)
					.build();

			return Arrays.asList(xref, xref2);
		}
		  static class UniParcGetIdParameterResolver extends AbstractGetIdParameterResolver {

		        @Override
		        public GetIdParameter validIdParameter() {
		            return GetIdParameter.builder().id(UPI)
		                    .resultMatcher(jsonPath("$.uniParcId.value",is(UPI)))
//		                    .resultMatcher(jsonPath("$.scientificName",is("scientific")))
//		                    .resultMatcher(jsonPath("$.commonName",is("common")))
//		                    .resultMatcher(jsonPath("$.mnemonic",is("mnemonic")))
//		                    .resultMatcher(jsonPath("$.links",contains("link")))
		                    .build();
		        }

		        @Override
		        public GetIdParameter invalidIdParameter() {
		            return GetIdParameter.builder().id("INVALID")
		                    .resultMatcher(jsonPath("$.url",not(isEmptyOrNullString())))
		                    .resultMatcher(jsonPath("$.messages.*",contains("The 'upi' value has invalid format. It should be a valid UniParc UPI")))
		                    .build();
		        }

		        @Override
		        public GetIdParameter nonExistentIdParameter() {
		            return GetIdParameter.builder().id("UPI0000083A09")
		                    .resultMatcher(jsonPath("$.url",not(isEmptyOrNullString())))
		                    .resultMatcher(jsonPath("$.messages.*",contains("Resource not found")))
		                    .build();
		        }

		        @Override
		        public GetIdParameter withFilterFieldsParameter() {
		            return GetIdParameter.builder().id(UPI).fields("upi,organism")
		                    .resultMatcher(jsonPath("$.uniParcId.value",is(UPI)))
//		                    .resultMatcher(jsonPath("$.scientificName",is("scientific")))
//		                    .resultMatcher(jsonPath("$.commonName").doesNotExist())
//		                    .resultMatcher(jsonPath("$.mnemonic").doesNotExist())
//		                    .resultMatcher(jsonPath("$.links").doesNotExist())
		                    .build();
		        }

		        @Override
		        public GetIdParameter withInvalidFilterParameter() {
		            return GetIdParameter.builder().id(UPI).fields("invalid")
		                    .resultMatcher(jsonPath("$.url",not(isEmptyOrNullString())))
		                    .resultMatcher(jsonPath("$.messages.*", contains("Invalid fields parameter value 'invalid'")))
		                    .build();
		        }
		    }

		    static class UniParcGetIdContentTypeParamResolver extends AbstractGetIdContentTypeParamResolver {

		        @Override
		        public GetIdContentTypeParam idSuccessContentTypesParam() {
		            return GetIdContentTypeParam.builder()
		                    .id(UPI)
		                    .contentTypeParam(ContentTypeParam.builder()
		                            .contentType(MediaType.APPLICATION_JSON)
		                            .resultMatcher(jsonPath("$.uniParcId.value",is(UPI)))
//		                            .resultMatcher(jsonPath("$.scientificName",is("scientific")))
//		                            .resultMatcher(jsonPath("$.commonName",is("common")))
//		                            .resultMatcher(jsonPath("$.mnemonic",is("mnemonic")))
//		                            .resultMatcher(jsonPath("$.links",contains("link")))
		                            .build())
		                    .contentTypeParam(ContentTypeParam.builder()
	                        .contentType(MediaType.APPLICATION_XML)
	                        .resultMatcher(content().string(containsString(UPI)))
//	                        .resultMatcher(jsonPath("$.scientificName",is("scientific")))
//	                        .resultMatcher(jsonPath("$.commonName",is("common")))
//	                        .resultMatcher(jsonPath("$.mnemonic",is("mnemonic")))
//	                        .resultMatcher(jsonPath("$.links",contains("link")))
	                        .build())
		                    .contentTypeParam(ContentTypeParam.builder()
		                            .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
		                            .resultMatcher(content().string(containsString(UPI)))
		                            .build())
		                    .contentTypeParam(ContentTypeParam.builder()
		                            .contentType(UniProtMediaType.FASTA_MEDIA_TYPE)
		                            .resultMatcher(content().string(containsString(UPI)))
		                            .build())
		                    .contentTypeParam(ContentTypeParam.builder()
		                            .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
		                            .resultMatcher(content().string(containsString("Entry\tOrganisms\tUniProtKB\tFirst seen\tLast seen\tLength")))
		                            .resultMatcher(content().string(containsString("UPI0000083A08	Homo sapiens; MOUSE	P12345; P52346	2017-02-12	2017-04-23	30")))
		                            .build())
		                    .contentTypeParam(ContentTypeParam.builder()
		                            .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
		                            .resultMatcher(content().contentType(UniProtMediaType.XLS_MEDIA_TYPE))
		                            .build())
		                    .build();
		        }

		        @Override
		        public GetIdContentTypeParam idBadRequestContentTypesParam() {
		            return GetIdContentTypeParam.builder()
		                    .id("INVALID")
		                    .contentTypeParam(ContentTypeParam.builder()
		                            .contentType(MediaType.APPLICATION_JSON)
		                            .resultMatcher(jsonPath("$.url",not(isEmptyOrNullString())))
		                      //      .resultMatcher(jsonPath("$.messages.*",contains("The 'upid' value has invalid format. It should be a valid Proteome UPID")))
		                            .build())
		                    .contentTypeParam(ContentTypeParam.builder()
		                            .contentType(MediaType.APPLICATION_XML)
		                            .resultMatcher(content().string(containsString("The 'upi' value has invalid format. It should be a valid UniParc UPI")))
		                            .build())
		                    .contentTypeParam(ContentTypeParam.builder()
		                            .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
		                            .resultMatcher(content().string(isEmptyString()))
		                            .build())
		                    .contentTypeParam(ContentTypeParam.builder()
		                            .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
		                            .resultMatcher(content().string(isEmptyString()))
		                            .build())
		                    .contentTypeParam(ContentTypeParam.builder()
		                            .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
		                            .resultMatcher(content().string(isEmptyString()))
		                            .build())
		                    .contentTypeParam(ContentTypeParam.builder()
		                            .contentType(UniProtMediaType.FASTA_MEDIA_TYPE)
		                            .resultMatcher(content().string(isEmptyString()))
		                            .build())
		                    .build();
		        }
		    }
}

