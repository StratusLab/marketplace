package eu.stratuslab.marketplace.server.cfg;

public class ServiceParameter {

    Long id;

    private ServiceParameter() {
    }

    public Long getId() {
        return id;
    }

    protected void setId(Long id) {
        this.id = id;
    }

    public String getValue() {
        // FIXME: Need real implementation.
        return "";
    }

}
