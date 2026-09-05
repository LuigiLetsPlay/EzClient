package app.ezclient.gui;

public interface ItemPhysicsState {
    void ezclient$physics(boolean active, float pitch, float yaw, float lift);
    boolean ezclient$active();
    float ezclient$pitch();
    float ezclient$yaw();
    float ezclient$lift();
}
