package myau.management;

public interface ITruePositionEntity {
    double getLerpX();

    void setLerpX(double value);

    double getLerpY();

    void setLerpY(double value);

    double getLerpZ();

    void setLerpZ(double value);

    double getTrueX();

    void setTrueX(double value);

    double getTrueY();

    void setTrueY(double value);

    double getTrueZ();

    void setTrueZ(double value);

    boolean hasTruePosition();

    void setTruePosition(boolean value);
}
