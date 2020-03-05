 package org.uniprot.api.uniprotkb.controller.download.IT;

 import org.junit.jupiter.api.BeforeEach;
 import org.junit.jupiter.api.extension.ExtendWith;
 import org.junit.jupiter.api.extension.RegisterExtension;
 import org.junit.jupiter.params.ParameterizedTest;
 import org.junit.jupiter.params.provider.Arguments;
 import org.junit.jupiter.params.provider.MethodSource;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.beans.factory.annotation.Qualifier;
 import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
 import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
 import org.springframework.http.HttpStatus;
 import org.springframework.http.MediaType;
 import org.springframework.test.context.ActiveProfiles;
 import org.springframework.test.context.ContextConfiguration;
 import org.springframework.test.context.junit.jupiter.SpringExtension;
 import org.springframework.web.client.RestTemplate;
 import org.uniprot.api.rest.controller.param.DownloadParamAndResult;
 import org.uniprot.api.rest.output.UniProtMediaType;
 import org.uniprot.api.uniprotkb.UniProtKBREST;
 import org.uniprot.api.uniprotkb.controller.UniprotKBController;
 import org.uniprot.api.uniprotkb.controller.download.resolver.UniProtKBDownloadSortParamResolver;
 import org.uniprot.api.uniprotkb.repository.DataStoreTestConfig;

 import java.util.stream.Stream;

 @ContextConfiguration(classes = {DataStoreTestConfig.class, UniProtKBREST.class})
 @ActiveProfiles(profiles = "offline")
 @AutoConfigureWebClient
 @WebMvcTest(UniprotKBController.class)
 @ExtendWith(value = {SpringExtension.class})
 public class UniProtKBDownloadSortIT extends BaseUniprotKBDownloadIT {

     @RegisterExtension
     static UniProtKBDownloadSortParamResolver paramResolver = new UniProtKBDownloadSortParamResolver();

     @Qualifier("rdfRestTemplate")
     @Autowired
     private RestTemplate restTemplate;

    @BeforeEach
    public void setUpData() {
        // when
        saveEntry(ACC1, 1);
        saveEntry(ACC2, 2);
        saveEntry(ACC3, 3);
    }

     @ParameterizedTest(name = "[{index}]~/download?{0}")
     @MethodSource("length")
     void testDownloadSortByLength(DownloadParamAndResult paramAndResult) throws Exception {
         sendAndVerify(paramAndResult, HttpStatus.OK);
     }

     @ParameterizedTest(name = "[{index}]~/download?{0}")
     @MethodSource("annotation_score")
     void testDownloadSortByAnnotationScore(DownloadParamAndResult paramAndResult) throws Exception {
         sendAndVerify(paramAndResult, HttpStatus.OK);

     }

     @ParameterizedTest(name = "[{index}]~/download?{0}")
     @MethodSource("accession")
     void testDownloadSortByAccession(DownloadParamAndResult paramAndResult) throws Exception {
         sendAndVerify(paramAndResult, HttpStatus.OK);

     }

     @ParameterizedTest(name = "[{index}]~/download?{0}")
     @MethodSource("mnemonic")
     void testDownloadSortByMnemonic(DownloadParamAndResult paramAndResult) throws Exception {
         sendAndVerify(paramAndResult, HttpStatus.OK);
     }

     @ParameterizedTest(name = "[{index}]~/download?{0}")
     @MethodSource("mass")
     void testDownloadSortByMass(DownloadParamAndResult paramAndResult) throws Exception {
         sendAndVerify(paramAndResult, HttpStatus.OK);

     }

     @ParameterizedTest(name = "[{index}]~/download?{0}")
     @MethodSource("name")
     void testDownloadSortByName(DownloadParamAndResult paramAndResult) throws Exception {
         sendAndVerify(paramAndResult, HttpStatus.OK);
     }

     @ParameterizedTest(name = "[{index}]~/download?{0}")
     @MethodSource("gene")
     void testDownloadSortByGene(DownloadParamAndResult paramAndResult) throws Exception {
         sendAndVerify(paramAndResult, HttpStatus.OK);

     }


     @ParameterizedTest(name = "[{index}]~/download?{0}")
     @MethodSource("organism_name")
     void testDownloadSortByOrganismName(DownloadParamAndResult paramAndResult) throws Exception {
         sendAndVerify(paramAndResult, HttpStatus.OK);

     }

     @ParameterizedTest(name = "[{index}]~/download?{0}")
     @MethodSource("taxonomy_name")
     void testDownloadSortByTaxonomyNameInvalid(DownloadParamAndResult paramAndResult) throws Exception {
         sendAndVerify(paramAndResult, HttpStatus.BAD_REQUEST);

     }

     @ParameterizedTest(name = "[{index}]~/download?{0}")
     @MethodSource("unknown_field")
     void testDownloadSortByUnknownField(DownloadParamAndResult paramAndResult) throws Exception {
         sendAndVerify(paramAndResult, HttpStatus.BAD_REQUEST);

     }

     @ParameterizedTest(name = "[{index}]~/download?{0}")
     @MethodSource("wrong_sort_order")
     void testDownloadSortByWrongSortOrder(DownloadParamAndResult paramAndResult) throws Exception {
         sendAndVerify(paramAndResult, HttpStatus.BAD_REQUEST);
     }

     private static Stream<Arguments> wrong_sort_order() {
         return requestResponseForSort("accession", "ascending");
     }

     private static Stream<Arguments> unknown_field() {
         return requestResponseForSort("unknown_field", "asc");
     }

     private static Stream<Arguments> taxonomy_name() {
         return requestResponseForSort("taxonomy_name", "asc");
     }

     private static Stream<Arguments> annotation_score() {
         return requestResponseForSort("annotation_score", "asc");
     }

     private static Stream<Arguments> accession() {
         return requestResponseForSort("accession", "desc");
     }

     private static Stream<Arguments> mnemonic() {
         return requestResponseForSort("mnemonic", "asc");
     }

     private static Stream<Arguments> name() {
         return requestResponseForSort("name", "asc");
     }

     private static Stream<Arguments> gene() {
         return requestResponseForSort("gene", "asc");
     }

     private static Stream<Arguments> organism_name() {
         return requestResponseForSort("organism_name", "asc");
     }

     private static Stream<Arguments> mass() {
         return requestResponseForSort("mass", "asc");
     }

     private static Stream<Arguments> length() {
         return requestResponseForSort("length", "asc");
     }

     private static Stream<Arguments> requestResponseForSort(String fieldName, String sortOrder){
         return Stream.of(
                 Arguments.of(
                         paramResolver.getDownloadWithSortParamAndResult(UniProtMediaType.TSV_MEDIA_TYPE,
                                 fieldName, sortOrder)),
                 Arguments.of(paramResolver.getDownloadWithSortParamAndResult(UniProtMediaType.FF_MEDIA_TYPE,
                         fieldName, sortOrder)),
                 Arguments.of(paramResolver.getDownloadWithSortParamAndResult(
                         UniProtMediaType.LIST_MEDIA_TYPE, fieldName, sortOrder)),
                 Arguments.of(paramResolver.getDownloadWithSortParamAndResult(MediaType.APPLICATION_XML,
                         fieldName, sortOrder)),
                 Arguments.of(paramResolver.getDownloadWithSortParamAndResult(MediaType.APPLICATION_JSON,
                         fieldName, sortOrder)),
                 Arguments.of(paramResolver.getDownloadWithSortParamAndResult(
                         UniProtMediaType.XLS_MEDIA_TYPE, fieldName, sortOrder)),
                 Arguments.of(paramResolver.getDownloadWithSortParamAndResult(
                         UniProtMediaType.FASTA_MEDIA_TYPE, fieldName, sortOrder)),
                 Arguments.of(paramResolver.getDownloadWithSortParamAndResult(
                         UniProtMediaType.GFF_MEDIA_TYPE, fieldName, sortOrder)),
                 Arguments.of(paramResolver.getDownloadWithSortParamAndResult(
                         UniProtMediaType.RDF_MEDIA_TYPE, fieldName, sortOrder)));
     }

 }
