package fr.cnes.regards.modules.acquisition.service.session;

import com.google.common.base.Strings;
import fr.cnes.regards.modules.acquisition.domain.Product;
import fr.cnes.regards.modules.acquisition.domain.ProductState;
import fr.cnes.regards.modules.ingest.dto.ISipState;

public class SessionChangingStateProbe {

    // Gathers info about initial product
    private String initialSession;

    private String initialSessionOwner;

    private String productName;

    private ProductState initialProductState;

    private ISipState initialProductSIPState;

    // Gathers info about updated product
    private String ingestionChain;

    private String session;

    private String sessionOwner;

    private ProductState productState;

    private ISipState productSIPState;

    public void addUpdatedProduct(Product updatedProduct) {
        ingestionChain = updatedProduct.getProcessingChain().getLabel();
        session = updatedProduct.getSession();
        sessionOwner = updatedProduct.getProcessingChain().getLabel();
        productState = updatedProduct.getState();
        productSIPState = updatedProduct.getSipState();
    }

    public static SessionChangingStateProbe build(Product initialProduct) {
        SessionChangingStateProbe sessionChangingStateProbe = new SessionChangingStateProbe();
        if (initialProduct != null) {
            sessionChangingStateProbe.productName = initialProduct.getProductName();
            sessionChangingStateProbe.initialSession = initialProduct.getSession();
            sessionChangingStateProbe.initialSessionOwner = initialProduct.getProcessingChain().getLabel();
            sessionChangingStateProbe.initialProductState = initialProduct.getState();
            sessionChangingStateProbe.initialProductSIPState = initialProduct.getSipState();
        }
        return sessionChangingStateProbe;
    }

    public boolean isSessionChanged() {
        return !Strings.isNullOrEmpty(initialSession) && !session.equals(initialSession);
    }

    public boolean shouldUpdateState() {
        return ((getInitialProductState() != getProductState()) && !(
            // Ignore FINISHED -> COMPLETED state change
            (getProductState() == ProductState.FINISHED) && (getInitialProductState() == ProductState.COMPLETED)));
    }

    public String getInitialSession() {
        return initialSession;
    }

    public ProductState getInitialProductState() {
        return initialProductState;
    }

    public ISipState getInitialProductSIPState() {
        return initialProductSIPState;
    }

    public String getIngestionChain() {
        return ingestionChain;
    }

    public String getSession() {
        return session;
    }

    public ProductState getProductState() {
        return productState;
    }

    public ISipState getProductSIPState() {
        return productSIPState;
    }

    public String getInitialSessionOwner() {
        return initialSessionOwner;
    }

    public String getProductName() {
        return productName;
    }

    public String getSessionOwner() {
        return sessionOwner;
    }

}
