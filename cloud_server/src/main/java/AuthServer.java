import java.util.HashMap;
import java.util.Map;

public class AuthServer {

    private static Map<String, String> loginMap = new HashMap<>();
    static {
        loginMap.put("roman","pass");
        loginMap.put("puttor","pass");
        loginMap.put("gitter","pass");
    }

    public boolean authSuccess(String login, String password){
        String tempPass = loginMap.get(login);
        if (tempPass!=null && tempPass.equals(password)){
            return true;
        }else{
            return false;
        }

    }

}
