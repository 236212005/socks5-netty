package agent.socks5.auth;

import cn.hutool.core.util.StrUtil;

import java.io.IOException;
import java.util.Properties;

public class PropertiesPasswordAuth implements PasswordAuth {

    private static final Properties properties;

    static {
        properties = new Properties();
        try {
            properties.load(PropertiesPasswordAuth.class.getResourceAsStream("/password.properties"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean auth(String user, String password) {
        //TODO CHANGE TO USE DATABASE
        String configPassword = properties.getProperty(user);
        return StrUtil.isNotBlank(configPassword) && password.equals(configPassword);
    }

}
