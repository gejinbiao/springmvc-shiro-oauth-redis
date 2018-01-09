oauth2.0客户端与服务端 demo
============

项目介绍:
---------------
 开发环境: IntelliJ IDEA+ JDK1.7
        
1. Maven构建SpringMVC基础架构





##测试 OAuth2
###获得授权码
http://localhost:8080/server/authorize?client_id=c1ebe466-1cdc-4bd3-ab69-77c3561b9dee&response_type=code&redirect_uri=http://localhost:8081/test/getCode 请求地址



###获得令牌
  注意：请求方式 POST
http://localhost:8081/test/oauthCallback?client_id=c1ebe466-1cdc-4bd3-ab69-77c3561b9dee&grant_type=authorization_code&code={code}

authorization_code
###刷新token
  注意：请求方式 POST
http://localhost:8081/test/refreshToken?client_id=c1ebe466-1cdc-4bd3-ab69-77c3561b9dee&grant_type=refresh_token&refresh_token={refreshToken}