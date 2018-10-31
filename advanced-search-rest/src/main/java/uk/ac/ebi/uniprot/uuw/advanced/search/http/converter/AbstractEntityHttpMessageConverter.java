package uk.ac.ebi.uniprot.uuw.advanced.search.http.converter;

import org.springframework.http.MediaType;
import uk.ac.ebi.uniprot.uuw.advanced.search.http.context.MessageConverterContext;

import java.util.stream.Stream;

/**
 * Extends {@link AbstractUUWHttpMessageConverter} by implementing {@link AbstractEntityHttpMessageConverter#entitiesToWrite(MessageConverterContext)}
 * so that it returns the actual entity (not entity ID) stream.
 *
 * Created 31/10/18
 *
 * @author Edd
 */
public abstract class AbstractEntityHttpMessageConverter<C> extends AbstractUUWHttpMessageConverter<C, C> {

    public AbstractEntityHttpMessageConverter(MediaType mediaType) {
        super(mediaType);
    }

    @Override
    protected Stream<C> entitiesToWrite(MessageConverterContext<C> context) {
        return context.getEntities();
    }
}
