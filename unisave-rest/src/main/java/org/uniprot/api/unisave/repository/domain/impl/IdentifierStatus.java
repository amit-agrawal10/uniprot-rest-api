package org.uniprot.api.unisave.repository.domain.impl;

import lombok.Data;
import org.uniprot.api.unisave.repository.domain.AccessionEvent;
import org.uniprot.api.unisave.repository.domain.EventTypeEnum;

import javax.persistence.*;

/**
 * Created with IntelliJ IDEA. User: wudong Date: 12/11/2013 Time: 14:24 To change this template use
 * File | Settings | File Templates.
 */
@Entity(name = "IdentifierStatus")
@IdClass(IdentifierStatusId.class)
@Table(name = "DELETED_MERGED_ACCESSION_VIEW")
@NamedQueries({
    @NamedQuery(
            name = "IdentifierStatus.findByFirstColumn",
            query = "select i from IdentifierStatus i where i.sourceAccession =:acc")
})
@Data
public class IdentifierStatus implements AccessionEvent {

    public enum Query {
        findByFirstColumn;

        public String query() {
            return IdentifierStatus.class.getSimpleName() + "." + this.name();
        }
    }

    @Id
    @Column(name = "OPERATION", nullable = false)
    @Enumerated(EnumType.STRING)
    private EventTypeEnum eventType;

    @Id
    @Column(name = "ACCESSION_SUBJECT", nullable = false)
    private String sourceAccession;

    @Id
    @Column(name = "ACCESSION_OBJECT")
    private String targetAccession;

    // when the status change happened
    @ManyToOne
    @JoinColumn(name = "RELEASE_ID")
    private ReleaseImpl eventRelease;

    @Column(name = "WITHDRAWN")
    private String withdrawn_flag;

    @Column(name = "DELETION_REASON")
    private String deletion_reason;

    public boolean isWithdrawn() {
        return withdrawn_flag != null;
    }
}
