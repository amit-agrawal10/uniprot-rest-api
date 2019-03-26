package uk.ac.ebi.uniprot.configure.api.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ebi.uniprot.configure.api.service.UtilService;
import uk.ac.ebi.uniprot.configure.uniprot.domain.query.SolrJsonQuery;
import uk.ac.ebi.uniprot.rest.validation.ValidSolrQuerySyntax;

import javax.validation.constraints.NotNull;


@RestController
@RequestMapping("/util")
@Validated
public class UtilController {

    private UtilService service;

    public UtilController(UtilService service){
        this.service = service;
    }

    @GetMapping("/queryParser")
    public SolrJsonQuery parseSolrQuery(
                                        @NotNull(message = "{query.parameter.required}")
                                        @ValidSolrQuerySyntax(message = "{search.invalid.query}")
                                        String query) {
        return service.convertQuery(query);
    }

}
