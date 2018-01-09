package com.gjb.shiroTest.controller;


import com.gjb.shiroTest.entity.Client;
import com.gjb.shiroTest.service.ClientService;
import com.gjb.shiroTest.utils.Constans;
import com.gjb.shiroTest.utils.RedisUtil;
import org.apache.oltu.oauth2.as.issuer.MD5Generator;
import org.apache.oltu.oauth2.as.issuer.OAuthIssuerImpl;
import org.apache.oltu.oauth2.as.request.OAuthAuthzRequest;
import org.apache.oltu.oauth2.as.response.OAuthASResponse;
import org.apache.oltu.oauth2.common.OAuth;
import org.apache.oltu.oauth2.common.error.OAuthError;
import org.apache.oltu.oauth2.common.exception.OAuthProblemException;
import org.apache.oltu.oauth2.common.exception.OAuthSystemException;
import org.apache.oltu.oauth2.common.message.OAuthResponse;
import org.apache.oltu.oauth2.common.message.types.ResponseType;
import org.apache.oltu.oauth2.common.utils.OAuthUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import java.util.Set;

/**
 * @author gejinbiao@ucfgroup.com
 * @Title
 * @Description:
 * @Company: ucfgroup.com
 * @Created 2018-01-04
 */
@Controller
@RequestMapping("/server")
public class AuthorizeController {


    @Autowired
    private ClientService clientService;

    /**
     * redis操作
     */
    @Autowired
    private RedisUtil redisUtil;


    /*
       grant_type：表示使用的授权模式，必选项，此处的值固定为"authorization_code"
       code：表示上一步获得的授权码，必选项。
       redirect_uri：表示重定向URI，必选项，且必须与A步骤中的该参数值保持一致
       client_id：表示客户端ID，必选项
   */
    @RequestMapping("/authorize")
    public Object authorize(Model model, HttpServletRequest request) throws OAuthSystemException, URISyntaxException {

        //构建OAuth请求
        OAuthAuthzRequest oAuthAuthzRequest = null;
        try {
            oAuthAuthzRequest = new OAuthAuthzRequest(request);

            Client client = clientService.findByClientId(oAuthAuthzRequest.getClientId());
            // 根据传入的clientId 判断 客户端是否存在
            if(client == null) {
                OAuthResponse response = OAuthASResponse.errorResponse(HttpServletResponse.SC_BAD_REQUEST)
                        .setError(OAuthError.TokenResponse.INVALID_CLIENT)
                        .setErrorDescription("客户端验证失败，错误的client_id")
                        .buildJSONMessage();
                return new ResponseEntity(response.getBody(), HttpStatus.valueOf(response.getResponseStatus()));
            }

            // 判断用户是否登录
           /* Subject subject = SecurityUtils.getSubject();

            if(!subject.isAuthenticated()) {
                if(!login(subject, request)) {
                    model.addAttribute("client", clientService.findByClientId(oAuthAuthzRequest.getClientId()));
                    return "oauth2login";
                }
            }
            String username = (String) subject.getPrincipal();*/

            //生成授权码
            String authorizationCode = null;

            String responseType = oAuthAuthzRequest.getParam(OAuth.OAUTH_RESPONSE_TYPE);
            if(responseType.equals(ResponseType.CODE.toString())) {
                OAuthIssuerImpl oAuthIssuer = new OAuthIssuerImpl(new MD5Generator());
                authorizationCode = oAuthIssuer.authorizationCode();
                //authorizeService.addAuthCode(authorizationCode, username);
                //将code放到redis中并保存600秒 10分钟
                redisUtil.set(Constans.CODE_ADD + authorizationCode,authorizationCode,600);
            }


            // 进行OAuth响应构建
            OAuthASResponse.OAuthAuthorizationResponseBuilder builder = OAuthASResponse.authorizationResponse(request, HttpServletResponse.SC_FOUND);

            // 设置授权码
            builder.setCode(authorizationCode);
            // 根据客户端重定向地址
            String redirectURI = oAuthAuthzRequest.getParam(OAuth.OAUTH_REDIRECT_URI);
            // 构建响应
            OAuthResponse response = builder.location(redirectURI).buildQueryMessage();

            // 根据OAuthResponse 返回 ResponseEntity响应
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(new URI(response.getLocationUri()));
            return new ResponseEntity(headers, HttpStatus.valueOf(response.getResponseStatus()));
        } catch(OAuthProblemException e) {
            // 出错处理
            String redirectUri = e.getRedirectUri();
            if(OAuthUtils.isEmpty(redirectUri)) {
                return new ResponseEntity("OAuth callback url needs to be provided by client!!!", HttpStatus.NOT_FOUND);
            }
            // 返回错误消息
            final OAuthResponse response = OAuthASResponse.errorResponse(HttpServletResponse.SC_FOUND).error(e).location(redirectUri).buildQueryMessage();
            HttpHeaders headers = new HttpHeaders();
            headers.setLocation(new URI(response.getLocationUri()));
            return new ResponseEntity(headers, HttpStatus.valueOf(response.getResponseStatus()));
        }
    }

    private boolean login(Subject subject, HttpServletRequest request) {
        if("get".equalsIgnoreCase(request.getMethod())) {
            return false;
        }

        String username = request.getParameter("username");
        String password = request.getParameter("password");

        if(StringUtils.isEmpty(username) || StringUtils.isEmpty(password)) {
            return false;
        }

        UsernamePasswordToken token = new UsernamePasswordToken(username, password);

        try {
            subject.login(token);
            return true;
        }catch(Exception e){
            request.setAttribute("error","登录失败"+e.getClass().getName());
            return false;
        }
    }

}