package mblog.base.oauth;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import mblog.base.oauth.utils.EnumOauthTypeBean;
import mblog.base.oauth.utils.OathConfig;
import mblog.base.oauth.utils.OpenOauthBean;
import mblog.base.oauth.utils.TokenUtil;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class OauthDouban extends Oauth {
    private static final String AUTH_URL = "https://www.douban.com/service/auth2/auth";
    private static final String TOKEN_URL = "https://www.douban.com/service/auth2/token";
    private static final String USER_INFO_URL = "https://api.douban.com/v2/user/~me";

    public static OauthDouban me() {
        return new OauthDouban();
    }

    public OauthDouban() {
        setClientId(OathConfig.getValue("openid_douban"));
        setClientSecret(OathConfig.getValue("openkey_douban"));
        setRedirectUri(OathConfig.getValue("redirect_douban"));
    }

    public String getAuthorizeUrl(String state) throws UnsupportedEncodingException {
        Map params = new HashMap();
        params.put("response_type", "code");
        params.put("client_id", getClientId());
        params.put("redirect_uri", getRedirectUri());
        if (StringUtils.isNotBlank(state)) {
            params.put("state", state);
        }
        return super.getAuthorizeUrl("https://www.douban.com/service/auth2/auth", params);
    }

    public String getTokenByCode(String code) throws IOException, KeyManagementException, NoSuchAlgorithmException, NoSuchProviderException {
        Map params = new HashMap();
        params.put("code", code);
        params.put("client_id", getClientId());
        params.put("client_secret", getClientSecret());
        params.put("grant_type", "authorization_code");
        params.put("redirect_uri", getRedirectUri());
        String token = TokenUtil.getAccessToken(super.doPost("https://www.douban.com/service/auth2/token", params));
        log.debug(token);
        return token;
    }

    public JSONObject getUserInfo(String accessToken) throws IOException, KeyManagementException, NoSuchAlgorithmException, NoSuchProviderException {
        Map params = new HashMap();
        params.put("Authorization", "Bearer " + accessToken);
        String userInfo = super.doGetWithHeaders("https://api.douban.com/v2/user/~me", params);
        JSONObject dataMap = JSON.parseObject(userInfo);
        log.debug(dataMap.toJSONString());
        return dataMap;
    }

    public JSONObject getUserInfoByCode(String code) throws IOException, KeyManagementException, NoSuchAlgorithmException, NoSuchProviderException {
        String accessToken = getTokenByCode(code);
        if (StringUtils.isBlank(accessToken)) {
            return null;
        }
        JSONObject dataMap = getUserInfo(accessToken);
        dataMap.put("access_token", accessToken);
        log.debug(dataMap.toJSONString());
        return dataMap;
    }


    public OpenOauthBean getUserBeanByCode(String code)
            throws KeyManagementException, NoSuchAlgorithmException, NoSuchProviderException, IOException {
        OpenOauthBean openOauthBean = null;
        JSONObject userInfo = me().getUserInfoByCode(code);

        openOauthBean = new OpenOauthBean();
        String openid = userInfo.getString("uid");
        String accessToken = userInfo.getString("access_token");
        String nickname = userInfo.getString("name");
        String photoUrl = userInfo.getString("large_avatar");

        openOauthBean.setOauthCode(code);
        openOauthBean.setAccessToken(accessToken);
        openOauthBean.setExpireIn("");
        openOauthBean.setOauthUserId(openid);
        openOauthBean.setOauthType(EnumOauthTypeBean.TYPE_DOUBAN.getValue());
        openOauthBean.setUsername("DB" + openid.getBytes().hashCode());
        openOauthBean.setNickname(nickname);
        openOauthBean.setAvatar(photoUrl);

        return openOauthBean;
    }
}
