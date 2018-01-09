package com.gjb.shiroTest.controller;


import com.gjb.shiroTest.entity.Client;
import com.gjb.shiroTest.service.ClientService;
import com.gjb.shiroTest.utils.Constans;
import com.gjb.shiroTest.utils.RedisUtil;
import org.apache.commons.collections.map.HashedMap;
import org.apache.oltu.oauth2.as.issuer.MD5Generator;
import org.apache.oltu.oauth2.as.issuer.OAuthIssuer;
import org.apache.oltu.oauth2.as.issuer.OAuthIssuerImpl;
import org.apache.oltu.oauth2.as.request.OAuthTokenRequest;
import org.apache.oltu.oauth2.as.response.OAuthASResponse;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.error.OAuthError;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.apache.oltu.oauth2.common.message.types.ParameterStyle;
import org.apache.oltu.oauth2.common.utils.OAuthUtils;
import org.apache.oltu.oauth2.rs.request.OAuthAccessResourceRequest;
import org.apache.oltu.oauth2.rs.response.OAuthRSResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Enumeration;
import java.util.Map;

/**
 * @author gejb on 2017/10/15 14:59.
 * @description
 */
@Controller
public class AccessTokenController {

    private Logger logger = LoggerFactory.getLogger(this.getClass());


    @Autowired
    private ClientService clientService;

    //redis操作类
    @Autowired
    private RedisUtil redisUtil;


    /**
     * 获取token令牌
     *
     * @param request
     * @return
     * @throws OAuthSystemException
     * @throws UnsupportedEncodingException
     */
    @RequestMapping(value = "/accessToken", method = RequestMethod.POST)
    public HttpEntity token(HttpServletRequest request) throws OAuthSystemException, UnsupportedEncodingException {

        try {

            //打印参数
            String username = request.getParameter("username");
            System.out.println("值" + username);
            System.out.println("获取token");
            Enumeration enu = request.getParameterNames();
            while (enu.hasMoreElements()) {
                String paraName = (String) enu.nextElement();
                String b2 = request.getParameter(paraName);
                System.out.println(b2);
            }

            // 构建Oauth请求
            OAuthTokenRequest oAuthTokenRequest = new OAuthTokenRequest(request);

            //检查提交的客户端id是否正确
            Client client = clientService.findByClientId(oAuthTokenRequest.getClientId());
            if (client == null) {
                OAuthResponse response = OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
                        .setError(OAuthError.TokenResponse.INVALID_CLIENT)
                        .setErrorDescription("客户端验证失败，如错误的client_id/client_secret")
                        .buildJSONMessage();
                ResponseEntity responseEntity = new ResponseEntity(response.getBody(), HttpStatus.valueOf(response.getResponseStatus()));
                System.out.println(responseEntity);
                return responseEntity;
            }

            // 检查客户端安全Key是否正确
            Object clientSecret = clientService.findByClientSecret(oAuthTokenRequest.getClientSecret());
            if (clientSecret == null) {
                OAuthResponse response = OAuthASResponse.errorResponse(HttpServletResponse.SC_UNAUTHORIZED)
                        .setError(OAuthError.TokenResponse.UNAUTHORIZED_CLIENT)
                        .setErrorDescription("客户端验证失败，如错误的client_id/client_secret")
                        .buildJSONMessage();
                return new ResponseEntity(response.getBody(), HttpStatus.valueOf(response.getResponseStatus()));
            }


            String authCode = oAuthTokenRequest.getParam(OAuth.OAUTH_CODE);
            String code = redisUtil.get(Constans.CODE_ADD + authCode, null);
            //从redis中没有取到code
            if (null == code) {
                //HttpServletResponse.SC_BAD_REQUEST
                OAuthResponse response = OAuthASResponse.errorResponse(HttpServletResponse.SC_OK)
                        .setError(OAuthError.TokenResponse.INVALID_REQUEST)
                        .setErrorDescription("code只能使用一次")
                        .buildJSONMessage();
                return new ResponseEntity(response.getBody(), HttpStatus.valueOf(response.getResponseStatus()));
            }
            if (StringUtils.isEmpty(authCode) || !code.equals(authCode)) {
                OAuthResponse response = OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
                        .setError(OAuthError.TokenResponse.INVALID_REQUEST)
                        .setErrorDescription("code无效")
                        .buildJSONMessage();
                return new ResponseEntity(response.getBody(), HttpStatus.valueOf(response.getResponseStatus()));
            }
            //是否与redis中一样
            /*if (!code.equals(authCode)) {
                OAuthResponse response = OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
                        .setError(OAuthError.TokenResponse.INVALID_REQUEST)
                        .setErrorDescription("code无效")
                        .buildJSONMessage();
                return new ResponseEntity(response.getBody(), HttpStatus.valueOf(response.getResponseStatus()));
            }*/
            //从redis中删掉authCode  authCode只可以用一次
            redisUtil.del(Constans.CODE_ADD + authCode);

            //生成Access Token
            OAuthIssuer issuer = new OAuthIssuerImpl(new MD5Generator());
            String accessToken = issuer.accessToken();
            String refreshToken = issuer.refreshToken();

            //将token存到redis中
            redisUtil.set(Constans.REFRESH_TOKEN_ADD + refreshToken, accessToken, 7200);
            redisUtil.set(Constans.TOKEN_ADD + accessToken, accessToken, 7200);
            logger.info("获取token成功并进行保存:accessToken : " + accessToken + "  refreshToken: " + refreshToken);

            // 生成OAuth响应
            OAuthResponse oauthResponse = OAuthASResponse
                    .tokenResponse(HttpServletResponse.SC_OK)
                    .setAccessToken(accessToken)
                    .setExpiresIn("7200")
                    .setRefreshToken(refreshToken)
                    .buildJSONMessage();
            return new ResponseEntity(oauthResponse.getBody(), HttpStatus.valueOf(oauthResponse.getResponseStatus()));

        } catch (OAuthProblemException e) {

            OAuthResponse res = OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST).error(e).buildBodyMessage();
            return new ResponseEntity(res.getBody(), HttpStatus.valueOf(res.getResponseStatus()));
        }
    }


    /**
     * 刷新令牌
     *
     * @param request
     * @throws OAuthSystemException
     * @throws OAuthSystemException
     * @url http://localhost:8080/oauth2/refresh_token?client_id={AppKey}&grant_type=refresh_token&refresh_token={refresh_token}
     */
    @RequestMapping(value = "/refreshToken", method = RequestMethod.POST)
    public ResponseEntity refresh_token(HttpServletRequest request) throws OAuthSystemException {
        OAuthIssuer oauthIssuerImpl = new OAuthIssuerImpl(new MD5Generator());
        try {

            //打印参数
            System.out.println("刷新token");
            Enumeration enu = request.getParameterNames();
            while (enu.hasMoreElements()) {
                String paraName = (String) enu.nextElement();
                String b2 = request.getParameter(paraName);
                System.out.println("键：" + paraName + " 值:" + b2);
            }


            //构建oauth2请求
            OAuthTokenRequest oAuthTokenRequest = new OAuthTokenRequest(request);
            //检查提交的客户端id是否正确
            Client client = clientService.findByClientId(oAuthTokenRequest.getClientId());
            if (client == null) {
                OAuthResponse response = OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
                        .setError(OAuthError.TokenResponse.INVALID_CLIENT)
                        .setErrorDescription("客户端验证失败，如错误的client_id/client_secret")
                        .buildJSONMessage();
                ResponseEntity responseEntity = new ResponseEntity(response.getBody(), HttpStatus.valueOf(response.getResponseStatus()));
                return responseEntity;
            }
            //验证是否是refresh_token
            if (!GrantType.REFRESH_TOKEN.name().equalsIgnoreCase(oAuthTokenRequest.getParam(OAuth.OAUTH_GRANT_TYPE))) {
                OAuthResponse response = OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
                        .setError(OAuthError.TokenResponse.INVALID_GRANT)
                        .setErrorDescription("VERIFY_CLIENTID_FAIL")
                        .buildJSONMessage();
                return new ResponseEntity(response.getBody(), HttpStatus.valueOf(response.getResponseStatus()));
            }
            /*
            * 刷新access_token有效期
             access_token是调用授权关系接口的调用凭证，由于access_token有效期（目前为2个小时）较短，当access_token超时后，可以使用refresh_token进行刷新，access_token刷新结果有两种：
             1. 若access_token已超时，那么进行refresh_token会获取一个新的access_token，新的超时时间；
             2. 若access_token未超时，那么进行refresh_token不会改变access_token，但超时时间会刷新，相当于续期access_token。
             refresh_token拥有较长的有效期（30天），当refresh_token失效的后，需要用户重新授权。
            * */
            String cacheRefreshToken = redisUtil.get(Constans.REFRESH_TOKEN_ADD + oAuthTokenRequest.getRefreshToken(), null);
            //access_token已超时
            if (cacheRefreshToken == null) {
                //生成token
                final String accessToken = oauthIssuerImpl.accessToken();
                String refreshToken = oauthIssuerImpl.refreshToken();
                redisUtil.set(Constans.REFRESH_TOKEN_ADD + refreshToken, accessToken, 7200);
                redisUtil.set(Constans.TOKEN_ADD + accessToken, accessToken, 7200);
                logger.info("刷新token成功并进行保存:accessToken : " + accessToken + "  refreshToken: " + refreshToken);
                //构建oauth2授权返回信息
                OAuthResponse response = OAuthASResponse.tokenResponse(HttpServletResponse.SC_OK)
                        .setAccessToken(accessToken)
                        .setExpiresIn("7200")
                        .setRefreshToken(refreshToken)
                        .buildJSONMessage();
                return new ResponseEntity(response.getBody(), HttpStatus.valueOf(response.getResponseStatus()));
            }
            //access_token未超时
            redisUtil.set(Constans.REFRESH_TOKEN_ADD + oAuthTokenRequest.getRefreshToken(), cacheRefreshToken, 7200);
            redisUtil.set(Constans.REFRESH_TOKEN_ADD + cacheRefreshToken, cacheRefreshToken, 7200);
            //构建oauth2授权返回信息
            OAuthResponse oauthResponse = OAuthASResponse
                    .tokenResponse(HttpServletResponse.SC_OK)
                    .setAccessToken(cacheRefreshToken)
                    .setExpiresIn("7200")
                    .setRefreshToken(oAuthTokenRequest.getRefreshToken())
                    .buildJSONMessage();
            return new ResponseEntity(oauthResponse.getBody(), HttpStatus.valueOf(oauthResponse.getResponseStatus()));
        } catch (OAuthProblemException ex) {

            logger.error(ex.getMessage(), ex);
            OAuthResponse res = OAuthASResponse.errorResponse(HttpServletResponse.SC_UNAUTHORIZED).error(ex).buildBodyMessage();
            return new ResponseEntity(res.getBody(), HttpStatus.valueOf(res.getResponseStatus()));

        }
    }


    /**
     * 判断字符串的编码
     *
     * @param str
     * @return
     */
    public static String getEncoding(String str) {
        String encode[] = new String[]{
                "UTF-8",
                "ISO-8859-1",
                "GB2312",
                "GBK",
                "GB18030",
                "Big5",
                "Unicode",
                "ASCII"
        };
        for (int i = 0; i < encode.length; i++) {
            try {
                if (str.equals(new String(str.getBytes(encode[i]), encode[i]))) {
                    return encode[i];
                }
            } catch (Exception ex) {
            }
        }

        return "";
    }

    /**
     * 用户信息
     *
     * @param request
     * @return
     * @throws OAuthSystemException
     */
    @RequestMapping("/userInfo")
    public HttpEntity userInfo(HttpServletRequest request) throws OAuthSystemException {
        try {

            //构建OAuth资源请求
            OAuthAccessResourceRequest oauthRequest = new OAuthAccessResourceRequest(request, ParameterStyle.QUERY);
            //获取Access Token
            String accessToken = oauthRequest.getAccessToken();

            //验证Access Token
            String value = redisUtil.get(Constans.TOKEN_ADD + accessToken, null);
            if (value == null) {
                // 如果不存在/过期了，返回未验证错误，需重新验证
                OAuthResponse oauthResponse = OAuthRSResponse
                        .errorResponse(HttpServletResponse.SC_UNAUTHORIZED)
                        .setRealm("fxb")
                        .setError(OAuthError.ResourceResponse.INVALID_TOKEN)
                        .buildHeaderMessage();

                HttpHeaders headers = new HttpHeaders();
                headers.add(OAuth.HeaderType.WWW_AUTHENTICATE, oauthResponse.getHeader(OAuth.HeaderType.WWW_AUTHENTICATE));
                return new ResponseEntity(headers, HttpStatus.UNAUTHORIZED);
            }
            //返回用户名
            return new ResponseEntity("测试用户", HttpStatus.OK);
        } catch (OAuthProblemException e) {
            //检查是否设置了错误码
            String errorCode = e.getError();
            if (OAuthUtils.isEmpty(errorCode)) {
                OAuthResponse oauthResponse = OAuthRSResponse
                        .errorResponse(HttpServletResponse.SC_UNAUTHORIZED)
                        .setRealm("get_resource exception")
                        .buildHeaderMessage();

                /*HttpHeaders headers = new HttpHeaders();
                headers.add(OAuth.HeaderType.WWW_AUTHENTICATE, oauthResponse.getHeader(OAuth.HeaderType.WWW_AUTHENTICATE));
                return new ResponseEntity(headers, HttpStatus.UNAUTHORIZED);*/
                //return new ResponseEntity(oauthResponse.getBody(), HttpStatus.UNAUTHORIZED)
            }

            OAuthResponse oauthResponse = OAuthRSResponse
                    .errorResponse(HttpServletResponse.SC_UNAUTHORIZED)
                    .setRealm("get_resource exception")
                    .setError(e.getError())
                    .setErrorDescription(e.getDescription())
                    .setErrorUri(e.getUri())
                    .buildHeaderMessage();

            HttpHeaders headers = new HttpHeaders();
            headers.add(OAuth.HeaderType.WWW_AUTHENTICATE, oauthResponse.getHeader(OAuth.HeaderType.WWW_AUTHENTICATE));
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }
    }


    public static String getCode(String content, String format) throws UnsupportedEncodingException {
        byte[] bytes = content.getBytes(format);
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < bytes.length; i++) {
            sb.append(Integer.toHexString(bytes[i] & 0xff).toUpperCase() + " ");
        }

        return sb.toString();
    }
}
