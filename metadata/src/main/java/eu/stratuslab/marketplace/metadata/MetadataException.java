package eu.stratuslab.marketplace.metadata;

@SuppressWarnings("serial")
public class MetadataException extends RuntimeException {

    public MetadataException(String message) {
        super(message);
    }

    public MetadataException(Exception cause) {
        super(cause);
    }
}