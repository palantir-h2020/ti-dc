package models;

public class NetflowTenant {
    private int tenantId;

    public NetflowTenant(int tenantId) {
        this.tenantId = tenantId;
    }

    public int getTenantId() {
        return tenantId;
    }
}
