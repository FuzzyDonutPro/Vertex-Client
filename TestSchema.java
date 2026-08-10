package test;

import com.vertexai.config.VertexConfig;
import com.vertexai.config.ConfigSerializer;

public class TestSchema {
    public static void main(String[] args) {
        VertexConfig config = new VertexConfig();
        System.out.println(ConfigSerializer.serialize(config).toString());
    }
}
