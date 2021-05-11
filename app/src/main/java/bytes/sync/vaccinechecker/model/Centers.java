package bytes.sync.vaccinechecker.model;

import java.io.Serializable;
import java.util.List;

public class Centers implements Serializable {

    public List<Center> centers;

    public List<Center> getCenters() {
        return centers;
    }

    public void setCenters(List<Center> centers) {
        this.centers = centers;
    }
}
