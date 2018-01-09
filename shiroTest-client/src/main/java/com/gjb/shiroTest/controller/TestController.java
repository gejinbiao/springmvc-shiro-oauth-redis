package com.gjb.shiroTest.controller;

import org.apache.oltu.oauth2.client.OAuthClient;
import org.apache.oltu.oauth2.client.URLConnectionClient;
import org.apache.oltu.oauth2.client.request.OAuthBearerClientRequest;
import org.apache.oltu.oauth2.client.request.OAuthClientRequest;
import org.apache.oltu.oauth2.client.response.OAuthAccessTokenResponse;
import org.apache.oltu.oauth2.client.response.OAuthAuthzResponse;
import org.apache.oltu.oauth2.client.response.OAuthResourceResponse;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.types.GrantType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;

/**
 * @author gejinbiao@ucfgroup.com
 * @Title
 * @Description:
 * @Company: ucfgroup.com
 * @Created 2018-01-04
 */
@Controller
@RequestMapping("/test")
public class TestController {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    String accessTokenUrl = "http://localhost:8080/accessToken";
    String refreshTokenUrl = "http://localhost:8080/refreshToken";
    String clientId = "c1ebe466-1cdc-4bd3-ab69-77c3561b9dee";
    String clientSecret = "d8346ea2-6017-43ed-ad68-19c0f971738b";
    String redirectUrl = "http://localhost:8081/test/page";
    String refreshToken = "c096f7682ddc3ff1ca79f56c76debfaf";
    String resourceUrl = "http://localhost:8080/userInfo";

    /*
        response_type：表示授权类型，必选项，此处的值固定为"code"
        client_id：表示客户端的ID，必选项
        redirect_uri：表示重定向URI，可选项
        scope：表示申请的权限范围，可选项
        state：表示客户端的当前状态，可以指定任意值，认证服务器会原封不动地返回这个值
    */

    /**
     * get请求
     * @param request
     * @param modelMap
     * @return
     * @throws UnsupportedEncodingException
     * @url http://localhost:8080/server/authorize?client_id=c1ebe466-1cdc-4bd3-ab69-77c3561b9dee&response_type=code&redirect_uri=http://localhost:8081/test/getCode 请求地址
     */
    @RequestMapping(value = "/getCode",method = RequestMethod.GET)
    public String getCode(HttpServletRequest request, ModelMap modelMap) throws UnsupportedEncodingException {
        try {

            //获取code
            OAuthAuthzResponse oAuthAuthzResponse = OAuthAuthzResponse.oauthCodeAuthzResponse(request);
            String code = oAuthAuthzResponse.getCode();

           /* OAuthClientRequest oauthResponse = OAuthClientRequest
                    .authorizationLocation("http://localhost:8080/server/authorize")
                    .setResponseType(OAuth.OAUTH_CODE)
                    .setClientId(clientId)
                    .setRedirectURI("http://localhost:8080/test/oauthCallback")
                    .setScope("user,order")
                    .buildQueryMessage();

            return "redirect:"+oauthResponse.getLocationUri();*/
            return "redirect: /test/oauthCallback?code=" + code;
        } catch (OAuthProblemException e) {
            e.printStackTrace();
        /*} catch (OAuthSystemException e) {
            e.printStackTrace();
        }*/
            modelMap.put("token", "错误");
            return "index";
        }
    }


    /**
     * 获取token
     *
     * @param modelMap
     * @param request
     * @return
     * @throws OAuthSystemException
     * @throws OAuthProblemException
     * @throws ConnectException
     */
    @RequestMapping(value = "/oauthCallback", method = RequestMethod.GET)
    public String oauthCallback(ModelMap modelMap, HttpServletRequest request) throws OAuthProblemException {

        OAuthAuthzResponse oauthAuthzResponse = null;
        try {

            oauthAuthzResponse = OAuthAuthzResponse.oauthCodeAuthzResponse(request);
            String code = oauthAuthzResponse.getCode();

            OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());
            OAuthAccessTokenResponse oAuthResponse = null;
            OAuthClientRequest accessTokenRequest = OAuthClientRequest.tokenLocation(accessTokenUrl)
                    .setGrantType(GrantType.AUTHORIZATION_CODE)
                    .setClientId(clientId)
                    .setClientSecret(clientSecret)
                    .setCode(code)
                    .setRedirectURI(redirectUrl)
                    .buildQueryMessage();
            oAuthResponse = oAuthClient.accessToken(accessTokenRequest, OAuth.HttpMethod.POST);

            String accessToken = oAuthResponse.getAccessToken();
            String refreshToken = oAuthResponse.getRefreshToken();
            Long expiresIn = oAuthResponse.getExpiresIn();
            //请求资源
            OAuthResourceResponse resource = getResource(oAuthClient, oAuthResponse);
            String resBody = resource.getBody();
            logger.info("accessToken: " + accessToken + " refreshToken: " + refreshToken + " expiresIn: " + expiresIn + " resBody: " + resBody);
            modelMap.addAttribute("accessToken", "accessToken: " + accessToken + " resBody: " + resBody);
            modelMap.put("token", accessToken);
            modelMap.put("refreshToken", refreshToken);
            modelMap.put("expiresIn", expiresIn);
            modelMap.put("resBody", resBody);

        } catch (OAuthSystemException e) {
            e.printStackTrace();
            modelMap.put("token", e.getMessage());
            return "index";
        }

        return "index";
    }


    /**
     * 刷新token
     *
     * @param modelMap
     * @return
     * @throws OAuthSystemException
     * @throws OAuthProblemException
     * @throws ConnectException
     * @url http://localhost:8081/test/refreshToken?client_id=c1ebe466-1cdc-4bd3-ab69-77c3561b9dee&grant_type=refresh_token&refresh_token=c096f7682ddc3ff1ca79f56c76debfaf
     */
    @RequestMapping("/refreshToken")
    public String refreshToken(ModelMap modelMap) {


        OAuthClient oAuthClient = new OAuthClient(new URLConnectionClient());
        OAuthAccessTokenResponse oAuthResponse = null;

        OAuthClientRequest accessTokenRequest = null;
        try {
            accessTokenRequest = OAuthClientRequest.tokenLocation(refreshTokenUrl)
                    .setGrantType(GrantType.REFRESH_TOKEN)
                    .setClientId(clientId)
                    .setClientSecret(clientSecret)
                    .setRedirectURI(redirectUrl)
                    .setRefreshToken(refreshToken)
                    .buildQueryMessage();

            //error_description
            oAuthResponse = oAuthClient.accessToken(accessTokenRequest, OAuth.HttpMethod.POST);
            //token
            String accessToken = oAuthResponse.getAccessToken();
            //刷新token
            String refreshToken = oAuthResponse.getRefreshToken();
            //时间
            Long expiresIn = oAuthResponse.getExpiresIn();
            //请求资源
            OAuthResourceResponse resource = getResource(oAuthClient, oAuthResponse);
            String resBody = resource.getBody();
            logger.info("accessToken: " + accessToken + " refreshToken: " + refreshToken + " expiresIn: " + expiresIn + " resBody: " + resBody);
            modelMap.addAttribute("accessToken", "accessToken: " + accessToken + " resBody: " + resBody);
            modelMap.put("token", accessToken);
            modelMap.put("refreshToken", refreshToken);
            modelMap.put("expiresIn", expiresIn);
            modelMap.put("resBody", resBody);
        } catch (OAuthSystemException e) {
            e.printStackTrace();
            modelMap.put("token", e.getMessage());
            return "index";
        } catch (OAuthProblemException e) {
            e.printStackTrace();
            modelMap.put("token", e.getDescription());
            return "index";
        }
        return "index";
    }

    /**
     * 获取资源
     *
     * @param oAuthClient
     * @param oAuthResponse
     * @return
     * @throws OAuthSystemException
     * @throws OAuthProblemException
     */
    public OAuthResourceResponse getResource(OAuthClient oAuthClient, OAuthAccessTokenResponse oAuthResponse) throws OAuthSystemException, OAuthProblemException {
        //获得资源服务
        OAuthClientRequest bearerClientRequest = new OAuthBearerClientRequest(resourceUrl)
                .setAccessToken(oAuthResponse.getAccessToken())
                .buildQueryMessage();
        OAuthResourceResponse resourceResponse = oAuthClient.resource(bearerClientRequest, OAuth.HttpMethod.GET, OAuthResourceResponse.class);

        return resourceResponse;
    }


}
