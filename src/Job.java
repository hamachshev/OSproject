import java.io.Serializable;

public class Job implements Serializable {
    private String name;
    private char type;
    private int time;

    public Job(String name, char type, int time) {
        this.name = name;
        this.type = type;
        this.time = time;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public char getType() {
        return type;
    }

    public void setType(char type) {
        this.type = type;
    }

    public int getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return name + ": Job type: " + type;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Job) {
            if (((Job) obj).getName().equals(name) && ((Job) obj).getType() == type) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + type;
        return result;
    }
}
